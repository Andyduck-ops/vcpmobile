package com.naigebao.auth.repository

import com.naigebao.auth.qr.QrCodeManager
import com.naigebao.auth.qr.QrLoginPayload
import com.naigebao.auth.token.AuthTokens
import com.naigebao.auth.token.TokenManager

class AuthRepository(
    private val tokenManager: TokenManager,
    private val qrCodeManager: QrCodeManager
) {
    fun decodeQrPayload(raw: String): QrLoginPayload? = qrCodeManager.decode(raw)

    fun authenticate(payload: QrLoginPayload): AuthResult {
        if (payload.expiresAt < System.currentTimeMillis()) {
            return AuthResult.Failed("QR code expired")
        }
        tokenManager.saveServerConfig(payload.serverUrl, payload.vcpKey)
        tokenManager.saveTokens(
            AuthTokens(
                accessToken = payload.vcpKey,
                sessionToken = payload.vcpKey,
                expiresAt = payload.expiresAt
            )
        )
        return AuthResult.Success(payload.serverUrl, payload.vcpKey)
    }

    fun currentServerConfig(): ServerConfig? {
        return tokenManager.loadServerConfig()?.let { ServerConfig(it.serverUrl, it.vcpKey) }
    }

    fun buildWebSocketUrl(channel: AuthChannel = AuthChannel.VCP_LOG): String? {
        val config = currentServerConfig() ?: return null
        val base = config.serverUrl.trimEnd('/')
        val wsBase = when {
            base.startsWith("ws://") || base.startsWith("wss://") -> base
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            else -> "ws://$base"
        }
        val path = when (channel) {
            AuthChannel.VCP_LOG -> "/VCPlog/VCP_Key=${config.vcpKey}"
            AuthChannel.VCP_INFO -> "/vcpinfo/VCP_Key=${config.vcpKey}"
        }
        return wsBase + path
    }

    data class ServerConfig(val serverUrl: String, val vcpKey: String)

    enum class AuthChannel {
        VCP_LOG,
        VCP_INFO
    }

    sealed class AuthResult {
        data class Success(val serverUrl: String, val vcpKey: String) : AuthResult()
        data class Failed(val reason: String) : AuthResult()
    }
}
