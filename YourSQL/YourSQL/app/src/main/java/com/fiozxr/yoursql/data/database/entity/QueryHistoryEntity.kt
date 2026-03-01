package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "query_history")
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val databaseName: String,
    val query: String,
    val executedAt: Long = Instant.now().epochSecond,
    val executionTimeMs: Long = 0,
    val rowCount: Int? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
