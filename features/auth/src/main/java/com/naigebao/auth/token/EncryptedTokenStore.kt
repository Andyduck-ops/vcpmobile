package com.naigebao.auth.token

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedTokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putString(KEY_SESSION, tokens.sessionToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAt ?: -1L)
            .apply()
    }

    fun loadTokens(): AuthTokens? {
        val accessToken = prefs.getString(KEY_ACCESS, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH, null)
        val sessionToken = prefs.getString(KEY_SESSION, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1L).takeIf { it > 0 }
        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            sessionToken = sessionToken,
            expiresAt = expiresAt
        )
    }

    fun saveServerConfig(serverUrl: String, vcpKey: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_VCP_KEY, vcpKey)
            .apply()
    }

    fun loadServerConfig(): ServerConfig? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val vcpKey = prefs.getString(KEY_VCP_KEY, null) ?: return null
        return ServerConfig(serverUrl, vcpKey)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    data class ServerConfig(val serverUrl: String, val vcpKey: String)

    companion object {
        private const val PREFS_NAME = "naigebao_tokens"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_SESSION = "session_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_VCP_KEY = "vcp_key"
    }
}
