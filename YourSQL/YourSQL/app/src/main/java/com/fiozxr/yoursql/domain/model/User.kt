package com.fiozxr.yoursql.domain.model

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class User(
    val id: String,
    val email: String,
    val role: UserRole,
    val createdAt: Long,
    val lastLoginAt: Long?,
    val isActive: Boolean
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedLastLoginAt: String?
        get() = lastLoginAt?.let { formatTimestamp(it) }

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    companion object {
        fun create(email: String, password: String, role: UserRole = UserRole.USER): Pair<User, String> {
            val id = UUID.randomUUID().toString()
            val passwordHash = hashPassword(password)
            val user = User(
                id = id,
                email = email,
                role = role,
                createdAt = Instant.now().epochSecond,
                lastLoginAt = null,
                isActive = true
            )
            return Pair(user, passwordHash)
        }

        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
            return Base64.getEncoder().encodeToString(hash)
        }

        fun verifyPassword(password: String, passwordHash: String): Boolean {
            return hashPassword(password) == passwordHash
        }
    }
}

enum class UserRole {
    USER,
    ADMIN;

    fun toEntityString(): String = name.lowercase()

    companion object {
        fun fromEntityString(value: String): UserRole {
            return when (value.lowercase()) {
                "admin" -> ADMIN
                else -> USER
            }
        }
    }
}
