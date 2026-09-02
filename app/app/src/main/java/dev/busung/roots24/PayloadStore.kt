package dev.busung.roots24

import android.content.Context
import java.io.File

class PayloadStore(private val context: Context) {
    val helper: File
        get() = File(context.applicationInfo.nativeLibraryDir, "libcve43499root.so")
    val payload: File get() = File(context.filesDir, "cve-2026-43499-app.so")
    val ksudNext: File get() = File(context.filesDir, "ksud-dczb2-next")
    val logFile: File get() = File(context.filesDir, "exploit.log")

    fun stageFromAssets() {
        require(helper.canExecute()) { "helper missing from nativeLibraryDir" }
        copyAsset("cve-2026-43499-app.so", payload, executable = false)
        copyAsset("ksud-dczb2-next", ksudNext, executable = true)
    }

    private fun copyAsset(name: String, target: File, executable: Boolean) {
        context.assets.open(name).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (executable) {
            check(target.setExecutable(true, false)) { "chmod failed: ${target.name}" }
        }
    }
}
