package com.fiozxr.yoursql.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Bucket(
    val name: String,
    val isPublic: Boolean,
    val createdAt: Long,
    val totalSize: Long,
    val fileCount: Int,
    val files: List<StorageFile> = emptyList()
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedTotalSize: String
        get() = formatFileSize(totalSize)

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}

data class StorageFile(
    val id: String,
    val bucketName: String,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedUpdatedAt: String
        get() = formatTimestamp(updatedAt)

    val formattedSize: String
        get() = formatFileSize(size)

    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
