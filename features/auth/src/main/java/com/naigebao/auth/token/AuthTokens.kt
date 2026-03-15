package com.naigebao.auth.token

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val sessionToken: String? = null,
    val expiresAt: Long? = null
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        val expiry = expiresAt ?: return false
        return now >= expiry
    }
}
