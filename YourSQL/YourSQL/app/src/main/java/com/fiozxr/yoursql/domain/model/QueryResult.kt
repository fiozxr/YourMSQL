package com.fiozxr.yoursql.domain.model

data class QueryResult(
    val isSuccess: Boolean,
    val rows: List<Map<String, Any?>> = emptyList(),
    val columns: List<String> = emptyList(),
    val rowsAffected: Int = 0,
    val executionTimeMs: Long = 0,
    val errorMessage: String? = null,
    val isSelectQuery: Boolean = false
) {
    companion object {
        fun success(
            rows: List<Map<String, Any?>>,
            columns: List<String>,
            executionTimeMs: Long
        ): QueryResult {
            return QueryResult(
                isSuccess = true,
                rows = rows,
                columns = columns,
                executionTimeMs = executionTimeMs,
                isSelectQuery = true
            )
        }

        fun mutation(
            rowsAffected: Int,
            executionTimeMs: Long
        ): QueryResult {
            return QueryResult(
                isSuccess = true,
                rowsAffected = rowsAffected,
                executionTimeMs = executionTimeMs,
                isSelectQuery = false
            )
        }

        fun error(message: String, executionTimeMs: Long = 0): QueryResult {
            return QueryResult(
                isSuccess = false,
                errorMessage = message,
                executionTimeMs = executionTimeMs
            )
        }
    }
}
