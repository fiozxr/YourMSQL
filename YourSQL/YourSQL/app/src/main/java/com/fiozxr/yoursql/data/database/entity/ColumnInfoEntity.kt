package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "column_info",
    foreignKeys = [
        ForeignKey(
            entity = TableInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tableId", "name"], unique = true)]
)
data class ColumnInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableId: Long,
    val name: String,
    val type: String, // TEXT, INTEGER, REAL, BLOB, BOOLEAN
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false,
    val defaultValue: String? = null,
    val columnOrder: Int = 0
)
