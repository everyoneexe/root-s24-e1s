package dev.busung.roots24

import android.os.Build
import android.system.Os

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val fingerprint: String,
    val androidRelease: String,
    val sdk: Int,
) {
    val supported: Boolean
        get() = model == SUPPORTED_MODEL &&
            device == SUPPORTED_DEVICE &&
            kernelRelease.startsWith(SUPPORTED_KERNEL_PREFIX) &&
            fingerprint.contains(SUPPORTED_FINGERPRINT_MARK)

    companion object {
        const val SUPPORTED_MODEL = "SM-S921B"
        const val SUPPORTED_DEVICE = "e1s"
        const val SUPPORTED_KERNEL_PREFIX = "6.1.138-android14-11"
        const val SUPPORTED_FINGERPRINT_MARK = "S921BXXSDCZB2"

        fun current(): DeviceSnapshot {
            val uname = Os.uname()
            return DeviceSnapshot(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                device = Build.DEVICE,
                kernelRelease = uname.release,
                fingerprint = Build.FINGERPRINT,
                androidRelease = Build.VERSION.RELEASE,
                sdk = Build.VERSION.SDK_INT,
            )
        }
    }
}

const val SAFE_UPTIME_MILLIS = 90_000L

fun formatUptime(uptimeMillis: Long, context: android.content.Context): String {
    val totalSeconds = (uptimeMillis / 1000L).coerceAtLeast(0)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> context.getString(R.string.uptime_format_hours, hours, minutes, seconds)
        minutes > 0 -> context.getString(R.string.uptime_format_minutes, minutes, seconds)
        else -> context.getString(R.string.uptime_format_seconds, seconds)
    }
}
