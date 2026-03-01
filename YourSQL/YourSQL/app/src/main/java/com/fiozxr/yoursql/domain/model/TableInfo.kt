package com.fiozxr.yoursql.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TableInfo(
    val id: Long = 0,
    val databaseName: String,
    val name: String,
    val displayName: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val rowCount: Long,
    val columns: List<ColumnInfo> = emptyList()
) {
    val formattedCreatedAt: String
        get() = formatTimestamp(createdAt)

    val formattedUpdatedAt: String
        get() = formatTimestamp(updatedAt)

    val primaryKeyColumn: ColumnInfo?
        get() = columns.find { it.isPrimaryKey }

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .format(Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()))
    }

    fun toCreateTableSql(): String {
        val columnsSql = columns.joinToString(", ") { it.toColumnDefSql() }
        return "CREATE TABLE IF NOT EXISTS $name ($columnsSql)"
    }
}

data class ColumnInfo(
    val id: Long = 0,
    val tableId: Long = 0,
    val name: String,
    val type: ColumnType,
    val isNullable: Boolean,
    val isPrimaryKey: Boolean,
    val defaultValue: String?,
    val columnOrder: Int
) {
    fun toColumnDefSql(): String {
        val sb = StringBuilder()
        sb.append("$name ${type.sqlName}")
        if (isPrimaryKey) {
            sb.append(" PRIMARY KEY")
        }
        if (!isNullable && !isPrimaryKey) {
            sb.append(" NOT NULL")
        }
        if (defaultValue != null) {
            sb.append(" DEFAULT $defaultValue")
        }
        return sb.toString()
    }

    companion object {
        fun create(
            name: String,
            type: ColumnType,
            isNullable: Boolean = true,
            isPrimaryKey: Boolean = false,
            defaultValue: String? = null,
            order: Int = 0
        ): ColumnInfo {
            return ColumnInfo(
                name = name,
                type = type,
                isNullable = isNullable,
                isPrimaryKey = isPrimaryKey,
                defaultValue = defaultValue,
                columnOrder = order
            )
        }
    }
}
