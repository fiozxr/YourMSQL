package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "database_info")
data class DatabaseInfoEntity(
    @PrimaryKey
    val name: String,
    val displayName: String,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond,
    val isActive: Boolean = false,
    val description: String? = null
)
