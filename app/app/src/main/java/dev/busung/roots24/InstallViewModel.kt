package dev.busung.roots24

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun stagedFileIsCurrent(staged: File, source: File): Boolean {
    if (!staged.exists()) return false
    val stagedDigest = sha256OrNull(staged) ?: return false
    return stagedDigest == sha256OrNull(source)
}

private fun sha256OrNull(file: File): String? = runCatching {
    file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}.getOrNull()

enum class InstallPhase {
    Checking,
    Ready,
    Staging,
    Exploiting,
    LoadingKernelSu,
    Installed,
    Failed,
}

data class InstallUiState(
    val phase: InstallPhase = InstallPhase.Checking,
    val message: String = "",
    val log: String = "",
    val supported: Boolean = false,
) {
    val busy: Boolean
        get() = phase in setOf(
            InstallPhase.Checking,
            InstallPhase.Staging,
            InstallPhase.Exploiting,
            InstallPhase.LoadingKernelSu,
        )
}

class InstallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val payloads = PayloadStore(application)
    private val historyStore = InstallHistoryStore(application)
    private val mutableState = MutableStateFlow(InstallUiState())
    private val mutableHistory = MutableStateFlow(historyStore.closeInterruptedRuns())
    private var installJob: Job? = null
    private var activeHistoryEntry: InstallHistoryEntry? = null
    @Volatile
    private var activeRunShizuku: Boolean? = null

    val state: StateFlow<InstallUiState> = mutableState.asStateFlow()
    val history: StateFlow<List<InstallHistoryEntry>> = mutableHistory.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (installJob?.isActive == true) return
        mutableHistory.value = historyStore.load()
        viewModelScope.launch(Dispatchers.IO) {
            val device = DeviceSnapshot.current()
            if (KernelSuProbe.isActive(app)) {
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Installed,
                    message = app.getString(R.string.status_rooted),
                    supported = device.supported,
                )
                return@launch
            }
            mutableState.value = InstallUiState(
                phase = if (device.supported) InstallPhase.Ready else InstallPhase.Failed,
                message = if (device.supported) {
                    app.getString(R.string.status_not_rooted)
                } else {
                    app.getString(R.string.status_unsupported)
                },
                supported = device.supported,
            )
        }
    }

    fun deleteHistoryEntries(ids: Collection<String>) {
        val runningId = activeHistoryEntry?.id
        val toDelete = ids.filterNot { it == runningId }
        if (toDelete.isEmpty()) return
        toDelete.forEach(historyStore::delete)
        mutableHistory.value = mutableHistory.value.filterNot { it.id in toDelete }
    }

    fun install() {
        if (installJob?.isActive == true || mutableState.value.phase == InstallPhase.Installed) return
        installJob = viewModelScope.launch(Dispatchers.IO) {
            mutableState.value = InstallUiState(
                phase = InstallPhase.Checking,
                message = app.getString(R.string.status_checking),
                supported = true,
            )
            startHistory()
            // Freeze the transport for the whole run so a mid-run preference
            // change cannot mix Shizuku and standalone execution.
            activeRunShizuku = AppPreferences.shizukuMode(app)
            try {
                if (shizukuEnabled()) {
                    appendLog(app.getString(R.string.log_shizuku_prepare))
                    if (!ShizukuController.isRunning() && !ShizukuController.pingUntilRunning()) {
                        error(app.getString(R.string.error_shizuku_unavailable))
                    }
                    if (!ShizukuController.isGranted() && !ShizukuController.requestPermission()) {
                        error(app.getString(R.string.error_shizuku_permission))
                    }
                    appendLog(app.getString(R.string.log_shizuku_permission))
                }
                val device = DeviceSnapshot.current()
                require(device.supported) { app.getString(R.string.error_unsupported_device) }
                appendLog("[+] ${device.model} ${device.device} ${device.kernelRelease}")

                setPhase(InstallPhase.Staging, app.getString(R.string.status_staging))
                payloads.stageFromAssets()
                appendLog("[+] staged ${payloads.helper.name}, ${payloads.payload.name}, ${payloads.ksudNext.name}")

                setPhase(InstallPhase.Exploiting, app.getString(R.string.status_exploit_running))
                runExploit()

                setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.status_ksu_loading))
                InstallReceipt.store(app)
                when {
                    KernelSuProbe.isVisibleToApp() -> appendLog("[+] kernelsu Live")
                    KernelSuProbe.isVisibleThroughShizuku() ->
                        appendLog("[+] kernelsu Live (Shizuku)")
                    else -> appendLog(
                        "[+] KernelSU Next late-load ok; /proc/modules hidden from app UID",
                    )
                }

                setPhase(InstallPhase.Installed, app.getString(R.string.status_rooted))
                appendLog("[+] ${app.getString(R.string.log_install_complete)}")
                finishHistory(InstallRunResult.Succeeded)
            } catch (error: Throwable) {
                appendLog("[-] ${error.message ?: error.javaClass.simpleName}")
                setPhase(InstallPhase.Failed, app.getString(R.string.status_install_failed))
                finishHistory(InstallRunResult.Failed)
            } finally {
                activeRunShizuku = null
            }
        }
    }

    private suspend fun runExploit() {
        val shizuku = shizukuEnabled()
        val helper = helperFile()
        val payload = if (shizuku) {
            shizukuStage(payloads.payload, SHIZUKU_PAYLOAD_PATH, "755")
        } else {
            payloads.payload
        }
        val ksud = if (shizuku) {
            shizukuStage(payloads.ksudNext, SHIZUKU_KSUD_PATH, "755")
        } else {
            payloads.ksudNext
        }
        val logFile = if (shizuku) File(SHIZUKU_LOG_PATH) else payloads.logFile
        if (shizuku) {
            ShizukuController.exec(arrayOf("rm", "-f", SHIZUKU_LOG_PATH)).waitFor()
        } else {
            logFile.delete()
            require(helper.canExecute()) { app.getString(R.string.error_helper_unavailable) }
        }
        val captured = StringBuilder()
        val logPrefix = mutableState.value.log
        val process = if (shizuku) {
            ShizukuController.exec(
                arrayOf(
                    helper.absolutePath,
                    "--run-payload",
                    payload.absolutePath,
                    helper.absolutePath,
                    logFile.absolutePath,
                ),
                shizukuEnvironment(helper.absolutePath, ksud.absolutePath),
            )
        } else {
            val processBuilder = ProcessBuilder(
                helper.absolutePath,
                "--run-payload",
                payload.absolutePath,
                helper.absolutePath,
                logFile.absolutePath,
            ).redirectErrorStream(true)
            processBuilder.environment().apply {
                put("EXPLOIT_ATTEMPTS", EXPLOIT_ATTEMPTS)
                put("P0_ATTEMPT_TIMEOUT_SEC", P0_ATTEMPT_TIMEOUT_SEC)
                put("EXPLOIT_ATTEMPT_TIMEOUT_SEC", EXPLOIT_ATTEMPT_TIMEOUT_SEC)
                put("CVE43499_ROOT_HELPER", helper.absolutePath)
                put("CVE43499_KSUD_NEXT", ksud.absolutePath)
            }
            processBuilder.start()
        }
        val readLog: () -> String = if (shizuku) {
            {
                drainProcessOutput(process, captured) + "\n" +
                    runCatching { ShizukuController.capture(arrayOf("cat", SHIZUKU_LOG_PATH)) }.getOrDefault("")
            }
        } else {
            { drainProcessOutput(process, captured) + logFile.readTextIfPresent() }
        }
        try {
            val startedAt = SystemClock.elapsedRealtime()
            var lastProgressAt = startedAt
            var lastRawLog = ""
            while (process.isAlive) {
                val rawLog = readLog()
                if (rawLog != lastRawLog) {
                    publishExploitLog(logPrefix, rawLog)
                    lastRawLog = rawLog
                    lastProgressAt = SystemClock.elapsedRealtime()
                }
                val now = SystemClock.elapsedRealtime()
                require(now - lastProgressAt < EXPLOIT_STALL_MILLIS) {
                    app.getString(R.string.error_exploit_stalled)
                }
                require(now - startedAt < EXPLOIT_TOTAL_MILLIS) {
                    app.getString(R.string.error_exploit_timeout)
                }
                delay(if (shizuku) SHIZUKU_LOG_POLL_INTERVAL else LOG_POLL_INTERVAL)
            }
            val exitCode = process.waitFor()
            val rawLog = readLog()
            publishExploitLog(logPrefix, rawLog)
            require(exitCode == 0) {
                app.getString(R.string.error_payload_exit, exitCode)
            }
            require(
                rawLog.contains("root kernelsu late-load ok=1") ||
                    (rawLog.contains("done=1 root=1") && KernelSuProbe.isActive(app)),
            ) {
                app.getString(R.string.error_success_marker)
            }
        } finally {
            if (process.isAlive) {
                process.destroy()
                delay(500.milliseconds)
                // Do not destroyForcibly: keepers may share the process group after a write.
            }
        }
        appendLog("[+] ${app.getString(R.string.log_bootstrap_root)}")
    }

    private fun helperFile(): File =
        if (shizukuEnabled()) {
            shizukuStage(payloads.helper, SHIZUKU_HELPER_PATH, "755")
        } else {
            payloads.helper
        }

    private fun shizukuEnabled(): Boolean = activeRunShizuku ?: AppPreferences.shizukuMode(app)

    private fun shizukuStage(source: File, target: String, mode: String): File {
        val staged = File(target)
        if (stagedFileIsCurrent(staged, source)) return staged
        try {
            ShizukuController.writeFile(target, mode, source.inputStream())
        } catch (error: Throwable) {
            throw IllegalStateException(
                app.getString(R.string.error_shizuku_stage, target, error.message.orEmpty()),
                error,
            )
        }
        return staged
    }

    private fun shizukuEnvironment(helperPath: String, ksudPath: String): Array<String> = arrayOf(
        "EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS",
        "P0_ATTEMPT_TIMEOUT_SEC=$P0_ATTEMPT_TIMEOUT_SEC",
        "EXPLOIT_ATTEMPT_TIMEOUT_SEC=$EXPLOIT_ATTEMPT_TIMEOUT_SEC",
        "CVE43499_ROOT_HELPER=$helperPath",
        "CVE43499_KSUD_NEXT=$ksudPath",
    )

    private fun drainProcessOutput(process: Process, buffer: StringBuilder): String {
        return try {
            drainStream(process.inputStream, buffer)
            drainStream(process.errorStream, buffer)
            buffer.toString()
        } catch (_: Throwable) {
            buffer.toString()
        }
    }

    private fun drainStream(stream: InputStream, buffer: StringBuilder) {
        val data = ByteArray(4096)
        while (stream.available() > 0) {
            val count = stream.read(data)
            if (count <= 0) break
            buffer.append(String(data, 0, count, Charsets.UTF_8))
        }
    }

    private fun publishExploitLog(prefix: String, rawLog: String) {
        mutableState.value = mutableState.value.copy(
            log = listOf(prefix, stripAnsi(rawLog)).filter(String::isNotBlank).joinToString("\n"),
        )
        updateHistoryLog()
    }

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableState.value = mutableState.value.copy(phase = phase, message = message)
        appendLog("[*] $message")
    }

    private fun appendLog(line: String) {
        val cleanLine = stripAnsi(line).trim()
        if (cleanLine.isBlank()) return
        mutableState.value = mutableState.value.copy(
            log = (mutableState.value.log + "\n" + cleanLine).trim(),
        )
        updateHistoryLog()
    }

    private fun startHistory() {
        val entry = historyStore.create()
        activeHistoryEntry = entry
        publishHistory(entry)
    }

    private fun updateHistory(transform: (InstallHistoryEntry) -> InstallHistoryEntry) {
        val entry = activeHistoryEntry ?: return
        val updated = transform(entry)
        activeHistoryEntry = updated
        historyStore.save(updated)
        publishHistory(updated)
    }

    private fun updateHistoryLog() = updateHistory { it.copy(log = mutableState.value.log) }

    private fun finishHistory(result: InstallRunResult) {
        updateHistory { entry ->
            entry.copy(
                completedAtMillis = System.currentTimeMillis(),
                result = result,
                log = mutableState.value.log,
            )
        }
        activeHistoryEntry = null
    }

    private fun publishHistory(entry: InstallHistoryEntry) {
        mutableHistory.value = (mutableHistory.value.filterNot { it.id == entry.id } + entry)
            .sortedByDescending(InstallHistoryEntry::startedAtMillis)
    }

    private fun File.readTextIfPresent(): String = if (exists()) readText() else ""

    companion object {
        private const val EXPLOIT_ATTEMPTS = "1"
        private const val P0_ATTEMPT_TIMEOUT_SEC = "300"
        private const val EXPLOIT_ATTEMPT_TIMEOUT_SEC = "420"
        private const val EXPLOIT_STALL_MILLIS = 1_200_000L
        private const val EXPLOIT_TOTAL_MILLIS = 1_200_000L
        private const val SHIZUKU_LOG_PATH = "/data/local/tmp/ksu-exploit.log"
        private const val SHIZUKU_HELPER_PATH = "/data/local/tmp/ksu-helper"
        private const val SHIZUKU_PAYLOAD_PATH = "/data/local/tmp/ksu-payload"
        private const val SHIZUKU_KSUD_PATH = "/data/local/tmp/ksud-dczb2-next"
        private val LOG_POLL_INTERVAL = 250.milliseconds
        private val SHIZUKU_LOG_POLL_INTERVAL = 1.seconds
        private val ANSI_ESCAPE = Regex("\\u001B\\[[0-?]*[ -/]*[@-~]")

        private fun stripAnsi(value: String): String = ANSI_ESCAPE.replace(value, "").replace("\r", "")
    }
}
