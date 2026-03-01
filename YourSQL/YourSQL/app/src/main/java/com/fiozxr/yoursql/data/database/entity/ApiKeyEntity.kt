package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey
    val key: String,
    val name: String,
    val scope: String, // read-only, read-write, admin
    val createdAt: Long = Instant.now().epochSecond,
    val lastUsedAt: Long? = null,
    val requestCount: Long = 0,
    val rateLimitPerMinute: Int = 100,
    val isActive: Boolean = true,
    val isDefault: Boolean = false
)
