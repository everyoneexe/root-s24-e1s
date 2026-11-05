package dev.busung.roots24

import android.content.Context
import android.os.SystemClock

object InstallReceipt {
    private const val PREFERENCES = "install_receipt"
    private const val BOOT_TOKEN = "kernel_boot_id"
    private const val INSTALLED_ELAPSED_MS = "installed_elapsed_ms"
    private const val ESTIMATED_BOOT_TIME_SEC = "estimated_boot_time_sec"
    private const val VERIFIED = "verified"

    fun isVerifiedForCurrentBoot(context: Context): Boolean {
        val receipt = prefs(context)
        if (!receipt.getBoolean(VERIFIED, false)) return false

        val recordedToken = receipt.getString(BOOT_TOKEN, null)
        val currentToken = KernelSuProbe.bootToken()
        if (recordedToken != null && currentToken != null) {
            return recordedToken == currentToken
        }

        val installedElapsed = receipt.getLong(INSTALLED_ELAPSED_MS, -1L)
        if (installedElapsed <= 0L) return false

        val currentElapsed = SystemClock.elapsedRealtime()
        if (currentElapsed < installedElapsed) {
            // SystemClock reset; device rebooted
            return false
        }

        val recordedBootTimeSec = receipt.getLong(ESTIMATED_BOOT_TIME_SEC, -1L)
        if (recordedBootTimeSec > 0L) {
            val currentBootTimeSec = (System.currentTimeMillis() - currentElapsed) / 1000L
            if (kotlin.math.abs(currentBootTimeSec - recordedBootTimeSec) > 120L) {
                return false
            }
        }
        return true
    }

    fun store(context: Context) {
        val currentElapsed = SystemClock.elapsedRealtime()
        val bootTimeSec = (System.currentTimeMillis() - currentElapsed) / 1000L
        val token = KernelSuProbe.bootToken()
        prefs(context)
            .edit()
            .apply {
                if (token != null) putString(BOOT_TOKEN, token)
                putLong(INSTALLED_ELAPSED_MS, currentElapsed)
                putLong(ESTIMATED_BOOT_TIME_SEC, bootTimeSec)
                putBoolean(VERIFIED, true)
            }
            .commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
