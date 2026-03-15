package com.naigebao.auth.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

data class DeviceFingerprint(
    val deviceId: String,
    val hashedId: String,
    val model: String,
    val manufacturer: String,
    val osVersion: String
)

object DeviceFingerprintCollector {
    fun collect(context: Context): DeviceFingerprint {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val raw = listOf(deviceId, Build.MODEL, Build.MANUFACTURER, Build.VERSION.SDK_INT.toString()).joinToString("|")
        return DeviceFingerprint(
            deviceId = deviceId,
            hashedId = sha256(raw),
            model = Build.MODEL ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown",
            osVersion = Build.VERSION.RELEASE ?: "unknown"
        )
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
