package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "buckets")
data class BucketEntity(
    @PrimaryKey
    val name: String,
    val isPublic: Boolean = false,
    val createdAt: Long = Instant.now().epochSecond,
    val totalSize: Long = 0,
    val fileCount: Int = 0
)
