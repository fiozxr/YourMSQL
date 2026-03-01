package com.fiozxr.yoursql.domain.model

enum class ColumnType(val sqlName: String, val kotlinType: String) {
    TEXT("TEXT", "String"),
    INTEGER("INTEGER", "Long"),
    REAL("REAL", "Double"),
    BLOB("BLOB", "ByteArray"),
    BOOLEAN("BOOLEAN", "Boolean");

    companion object {
        fun fromSqlName(name: String): ColumnType? {
            return entries.find { it.sqlName.equals(name, ignoreCase = true) }
        }
    }
}
