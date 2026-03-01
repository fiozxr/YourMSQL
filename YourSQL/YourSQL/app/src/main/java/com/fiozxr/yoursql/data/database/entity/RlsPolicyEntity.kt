package com.fiozxr.yoursql.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rls_policies",
    foreignKeys = [
        ForeignKey(
            entity = TableInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tableId", "scope"], unique = true)]
)
data class RlsPolicyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableId: Long,
    val scope: String, // read-only, read-write, admin
    val condition: String, // SQL WHERE clause fragment
    val description: String? = null
)
