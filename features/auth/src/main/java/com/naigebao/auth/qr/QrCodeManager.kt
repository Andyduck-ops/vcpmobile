package com.naigebao.auth.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class QrLoginPayload(
    val serverUrl: String,
    val vcpKey: String,
    val deviceId: String,
    val issuedAt: Long,
    val expiresAt: Long
)

class QrCodeManager(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {
    fun encode(payload: QrLoginPayload): String {
        return json.encodeToString(payload)
    }

    fun decode(raw: String): QrLoginPayload? {
        return runCatching { json.decodeFromString(QrLoginPayload.serializer(), raw) }.getOrNull()
    }

    fun generateBitmap(payload: QrLoginPayload, size: Int = DEFAULT_SIZE): Bitmap {
        val content = encode(payload)
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        return matrix.toBitmap()
    }

    fun generateImageBitmap(payload: QrLoginPayload, size: Int = DEFAULT_SIZE): ImageBitmap {
        return generateBitmap(payload, size).asImageBitmap()
    }

    private fun BitMatrix.toBitmap(): Bitmap {
        val width = width
        val height = height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (get(x, y)) COLOR_BLACK else COLOR_WHITE)
            }
        }
        return bitmap
    }

    companion object {
        const val DEFAULT_SIZE = 640
        private const val COLOR_BLACK = 0xFF000000.toInt()
        private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    }
}
