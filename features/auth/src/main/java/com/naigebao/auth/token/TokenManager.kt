package com.naigebao.auth.token

class TokenManager(
    private val tokenStore: EncryptedTokenStore
) {
    fun currentTokens(): AuthTokens? = tokenStore.loadTokens()

    fun saveTokens(tokens: AuthTokens) {
        tokenStore.saveTokens(tokens)
    }

    fun saveServerConfig(serverUrl: String, vcpKey: String) {
        tokenStore.saveServerConfig(serverUrl, vcpKey)
    }

    fun loadServerConfig(): EncryptedTokenStore.ServerConfig? = tokenStore.loadServerConfig()

    fun clear() {
        tokenStore.clear()
    }

    suspend fun ensureValidTokens(
        refresh: suspend (String) -> AuthTokens?
    ): AuthTokens? {
        val tokens = tokenStore.loadTokens() ?: return null
        if (!tokens.isExpired()) {
            return tokens
        }
        val refreshToken = tokens.refreshToken ?: return tokens
        val refreshed = refresh(refreshToken)
        if (refreshed != null) {
            tokenStore.saveTokens(refreshed)
            return refreshed
        }
        return tokens
    }
}
