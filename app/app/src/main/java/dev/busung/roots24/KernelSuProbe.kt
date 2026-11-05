package dev.busung.roots24

import android.content.Context
import java.io.File

object KernelSuProbe {
    fun isVisibleToApp(): Boolean {
        if (isSuBinaryWorking()) return true
        if (File("/sys/module/kernelsu").exists()) return true
        val modules = runCatching { File("/proc/modules").readText() }.getOrNull() ?: return false
        return modules.lineSequence().any { it.startsWith("kernelsu ") }
    }

    fun isSuBinaryWorking(): Boolean {
        val candidates = listOf("su", "/system/bin/su", "/system/xbin/su")
        for (cmd in candidates) {
            val ok = runCatching {
                val process = Runtime.getRuntime().exec(arrayOf(cmd, "-v"))
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                process.exitValue() == 0 && output.isNotEmpty()
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    fun isVisibleThroughShizuku(): Boolean {
        if (!ShizukuController.isGranted()) return false
        val suCheck = runCatching {
            val out = ShizukuController.capture(arrayOf("su", "-v")).trim()
            out.isNotEmpty()
        }.getOrDefault(false)
        if (suCheck) return true

        val modules = runCatching {
            ShizukuController.capture(arrayOf("cat", "/proc/modules"))
        }.getOrDefault("")
        if (modules.lineSequence().any { it.startsWith("kernelsu ") }) return true
        return runCatching {
            ShizukuController.exec(arrayOf("test", "-d", "/sys/module/kernelsu")).waitFor() == 0
        }.getOrDefault(false)
    }

    fun bootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id")
            .readText(Charsets.US_ASCII)
            .trim()
            .takeIf(String::isNotBlank)
    }.getOrNull()

    fun isActive(context: Context): Boolean {
        if (isVisibleToApp()) return true
        if (isVisibleThroughShizuku()) return true
        return InstallReceipt.isVerifiedForCurrentBoot(context)
    }
}
