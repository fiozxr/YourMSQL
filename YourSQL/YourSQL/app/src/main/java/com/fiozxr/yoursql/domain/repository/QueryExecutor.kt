package com.fiozxr.yoursql.domain.repository

import com.fiozxr.yoursql.domain.model.QueryResult

interface QueryExecutor {
    suspend fun executeQuery(databaseName: String, sql: String): QueryResult
    suspend fun executeSelect(
        databaseName: String,
        tableName: String,
        columns: List<String>? = null,
        whereClause: String? = null,
        orderBy: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): QueryResult

    suspend fun executeInsert(
        databaseName: String,
        tableName: String,
        values: Map<String, Any?>
    ): QueryResult

    suspend fun executeBatchInsert(
        databaseName: String,
        tableName: String,
        valuesList: List<Map<String, Any?>>
    ): QueryResult

    suspend fun executeUpdate(
        databaseName: String,
        tableName: String,
        values: Map<String, Any?>,
        whereClause: String
    ): QueryResult

    suspend fun executeDelete(
        databaseName: String,
        tableName: String,
        whereClause: String
    ): QueryResult

    suspend fun beginTransaction(databaseName: String)
    suspend fun commitTransaction(databaseName: String)
    suspend fun rollbackTransaction(databaseName: String)
}
