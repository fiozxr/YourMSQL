package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "storage_files",
    foreignKeys = [
        ForeignKey(
            entity = BucketEntity::class,
            parentColumns = ["name"],
            childColumns = ["bucketName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bucketName", "path"], unique = true)]
)
data class StorageFileEntity(
    @PrimaryKey
    val id: String,
    val bucketName: String,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond
)
