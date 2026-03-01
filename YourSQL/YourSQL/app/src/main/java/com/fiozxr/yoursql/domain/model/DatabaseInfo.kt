package com.fiozxr.yoursql.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DatabaseInfo(
    val name: String,
    val displayName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val description: String?
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedUpdatedAt: String
        get() = formatTimestamp(updatedAt)

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    companion object {
        fun create(name: String, displayName: String? = null, description: String? = null): DatabaseInfo {
            val now = Instant.now().epochSecond
            return DatabaseInfo(
                name = name,
                displayName = displayName ?: name,
                createdAt = now,
                updatedAt = now,
                isActive = false,
                description = description
            )
        }
    }
}
