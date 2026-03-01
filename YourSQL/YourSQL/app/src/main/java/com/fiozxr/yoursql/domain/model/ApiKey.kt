package com.fiozxr.yoursql.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ApiKeyScope {
    READ_ONLY,
    READ_WRITE,
    ADMIN;

    fun toEntityString(): String = name.lowercase().replace("_", "-")

    companion object {
        fun fromEntityString(value: String): ApiKeyScope {
            return when (value.lowercase()) {
                "read-only" -> READ_ONLY
                "read-write" -> READ_WRITE
                "admin" -> ADMIN
                else -> READ_ONLY
            }
        }
    }
}

data class ApiKey(
    val key: String,
    val name: String,
    val scope: ApiKeyScope,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val requestCount: Long,
    val rateLimitPerMinute: Int,
    val isActive: Boolean,
    val isDefault: Boolean
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedLastUsedAt: String?
        get() = lastUsedAt?.let { formatTimestamp(it) }

    val scopeDisplayName: String
        get() = when (scope) {
            ApiKeyScope.READ_ONLY -> "Read Only"
            ApiKeyScope.READ_WRITE -> "Read & Write"
            ApiKeyScope.ADMIN -> "Admin"
        }

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    companion object {
        fun generate(name: String, scope: ApiKeyScope = ApiKeyScope.READ_ONLY): ApiKey {
            val key = generateSecureKey()
            return ApiKey(
                key = key,
                name = name,
                scope = scope,
                createdAt = Instant.now().epochSecond,
                lastUsedAt = null,
                requestCount = 0,
                rateLimitPerMinute = 100,
                isActive = true,
                isDefault = false
            )
        }

        private fun generateSecureKey(): String {
            val bytes = java.security.SecureRandom().generateSeed(32)
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
