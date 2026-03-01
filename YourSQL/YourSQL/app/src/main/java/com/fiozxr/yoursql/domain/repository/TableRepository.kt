package com.fiozxr.yoursql.domain.repository

import com.fiozxr.yoursql.domain.model.ColumnInfo
import com.fiozxr.yoursql.domain.model.ColumnType
import com.fiozxr.yoursql.domain.model.TableInfo
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun getTables(databaseName: String): Flow<List<TableInfo>>
    suspend fun getTable(databaseName: String, tableName: String): TableInfo?
    suspend fun getTableById(tableId: Long): TableInfo?
    suspend fun createTable(
        databaseName: String,
        tableName: String,
        columns: List<ColumnInfo>
    ): Result<TableInfo>
    suspend fun dropTable(databaseName: String, tableName: String): Result<Unit>
    suspend fun renameTable(databaseName: String, oldName: String, newName: String): Result<Unit>
    suspend fun addColumn(
        databaseName: String,
        tableName: String,
        column: ColumnInfo
    ): Result<Unit>
    suspend fun dropColumn(databaseName: String, tableName: String, columnName: String): Result<Unit>
    suspend fun getTableRowCount(databaseName: String, tableName: String): Long
    suspend fun refreshTableMetadata(databaseName: String, tableName: String): Result<Unit>
    suspend fun syncSchemaFromDatabase(databaseName: String): Result<Unit>
}
