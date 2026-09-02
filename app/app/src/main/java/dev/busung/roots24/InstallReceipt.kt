package dev.busung.roots24

import android.content.Context

object InstallReceipt {
    private const val PREFERENCES = "install_receipt"
    private const val BOOT_TOKEN = "kernel_boot_id"
    private const val VERIFIED = "verified"

    fun isVerifiedForCurrentBoot(context: Context): Boolean {
        val bootToken = KernelSuProbe.bootToken() ?: return false
        val receipt = prefs(context)
        return receipt.getString(BOOT_TOKEN, null) == bootToken &&
            receipt.getBoolean(VERIFIED, false)
    }

    fun store(context: Context) {
        val bootToken = KernelSuProbe.bootToken() ?: return
        prefs(context)
            .edit()
            .putString(BOOT_TOKEN, bootToken)
            .putBoolean(VERIFIED, true)
            .commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
