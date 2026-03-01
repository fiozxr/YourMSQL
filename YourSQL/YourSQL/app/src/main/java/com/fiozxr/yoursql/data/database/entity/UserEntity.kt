package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val passwordHash: String,
    val role: String = "user", // user, admin
    val createdAt: Long = Instant.now().epochSecond,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)
