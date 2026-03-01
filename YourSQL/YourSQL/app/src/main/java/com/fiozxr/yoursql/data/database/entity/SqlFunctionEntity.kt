package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sql_functions")
data class SqlFunctionEntity(
    @PrimaryKey
    val name: String,
    val description: String? = null,
    val sqlTemplate: String,
    val parameters: String, // JSON array of parameter names
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond
)
