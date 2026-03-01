package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "table_info",
    foreignKeys = [
        ForeignKey(
            entity = DatabaseInfoEntity::class,
            parentColumns = ["name"],
            childColumns = ["databaseName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["databaseName", "name"], unique = true)]
)
data class TableInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val databaseName: String,
    val name: String,
    val displayName: String? = null,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond,
    val rowCount: Long = 0
)
