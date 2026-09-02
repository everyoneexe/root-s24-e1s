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
