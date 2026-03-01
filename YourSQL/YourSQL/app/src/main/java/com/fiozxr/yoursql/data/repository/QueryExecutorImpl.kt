package com.fiozxr.yoursql.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.QueryHistoryEntity
import com.fiozxr.yoursql.domain.model.QueryResult
import com.fiozxr.yoursql.domain.repository.QueryExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryExecutorImpl @Inject constructor(
    private val masterDb: MasterDatabase,
    private val databaseRepository: DatabaseRepositoryImpl
) : QueryExecutor {

    private val openDatabases = ConcurrentHashMap<String, SQLiteDatabase>()
    private val activeTransactions = ConcurrentHashMap<String, SQLiteDatabase>()

    override suspend fun executeQuery(databaseName: String, sql: String): QueryResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                val db = getDatabase(databaseName)
                val upperSql = sql.trim().uppercase()

                val result = when {
                    upperSql.startsWith("SELECT") || upperSql.startsWith("PRAGMA") -> {
                        executeSelectQuery(db, sql)
                    }

                    upperSql.startsWith("INSERT") -> {
                        executeInsertQuery(db, sql)
                    }

                    upperSql.startsWith("UPDATE") -> {
                        executeUpdateQuery(db, sql)
                    }

                    upperSql.startsWith("DELETE") -> {
                        executeDeleteQuery(db, sql)
                    }

                    upperSql.startsWith("CREATE") ||
                            upperSql.startsWith("DROP") ||
                            upperSql.startsWith("ALTER") -> {
                        executeSchemaQuery(db, sql)
                    }

                    else -> {
                        executeGenericQuery(db, sql)
                    }
                }

                val executionTime = System.currentTimeMillis() - startTime

                // Log to history
                logQueryHistory(databaseName, sql, executionTime, result)

                result.copy(executionTimeMs = executionTime)
            } catch (e: Exception) {
                val executionTime = System.currentTimeMillis() - startTime
                Timber.e(e, "Query execution failed: $sql")
                logQueryHistory(databaseName, sql, executionTime, null, e.message)
                QueryResult.error(e.message ?: "Unknown error", executionTime)
            }
        }

    override suspend fun executeSelect(
        databaseName: String,
        tableName: String,
        columns: List<String>?,
        whereClause: String?,
        orderBy: String?,
        limit: Int?,
        offset: Int?
    ): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val db = getDatabase(databaseName)

            val columnList = columns?.joinToString(", ") ?: "*"
            val sql = buildString {
                append("SELECT $columnList FROM $tableName")
                whereClause?.let { append(" WHERE $it") }
                orderBy?.let { append(" ORDER BY $it") }
                limit?.let { append(" LIMIT $it") }
                offset?.let { append(" OFFSET $it") }
            }

            val result = executeSelectQuery(db, sql)
            val executionTime = System.currentTimeMillis() - startTime

            result.copy(executionTimeMs = executionTime)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.error(e.message ?: "Unknown error", executionTime)
        }
    }

    override suspend fun executeInsert(
        databaseName: String,
        tableName: String,
        values: Map<String, Any?>
    ): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val db = getDatabase(databaseName)
            val contentValues = ContentValues()

            values.forEach { (key, value) ->
                when (value) {
                    null -> contentValues.putNull(key)
                    is String -> contentValues.put(key, value)
                    is Int -> contentValues.put(key, value)
                    is Long -> contentValues.put(key, value)
                    is Double -> contentValues.put(key, value)
                    is Float -> contentValues.put(key, value)
                    is Boolean -> contentValues.put(key, if (value) 1 else 0)
                    is ByteArray -> contentValues.put(key, value)
                    else -> contentValues.put(key, value.toString())
                }
            }

            val rowId = db.insert(tableName, null, contentValues)
            val executionTime = System.currentTimeMillis() - startTime

            if (rowId == -1L) {
                QueryResult.error("Insert failed", executionTime)
            } else {
                QueryResult.mutation(1, executionTime)
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.error(e.message ?: "Insert failed", executionTime)
        }
    }

    override suspend fun executeBatchInsert(
        databaseName: String,
        tableName: String,
        valuesList: List<Map<String, Any?>>
    ): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val db = getDatabase(databaseName)
            var successCount = 0

            db.beginTransaction()
            try {
                valuesList.forEach { values ->
                    val contentValues = ContentValues()
                    values.forEach { (key, value) ->
                        when (value) {
                            null -> contentValues.putNull(key)
                            is String -> contentValues.put(key, value)
                            is Int -> contentValues.put(key, value)
                            is Long -> contentValues.put(key, value)
                            is Double -> contentValues.put(key, value)
                            is Float -> contentValues.put(key, value)
                            is Boolean -> contentValues.put(key, if (value) 1 else 0)
                            is ByteArray -> contentValues.put(key, value)
                            else -> contentValues.put(key, value.toString())
                        }
                    }

                    val rowId = db.insert(tableName, null, contentValues)
                    if (rowId != -1L) successCount++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.mutation(successCount, executionTime)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.error(e.message ?: "Batch insert failed", executionTime)
        }
    }

    override suspend fun executeUpdate(
        databaseName: String,
        tableName: String,
        values: Map<String, Any?>,
        whereClause: String
    ): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val db = getDatabase(databaseName)
            val contentValues = ContentValues()

            values.forEach { (key, value) ->
                when (value) {
                    null -> contentValues.putNull(key)
                    is String -> contentValues.put(key, value)
                    is Int -> contentValues.put(key, value)
                    is Long -> contentValues.put(key, value)
                    is Double -> contentValues.put(key, value)
                    is Float -> contentValues.put(key, value)
                    is Boolean -> contentValues.put(key, if (value) 1 else 0)
                    is ByteArray -> contentValues.put(key, value)
                    else -> contentValues.put(key, value.toString())
                }
            }

            val rowsAffected = db.update(tableName, contentValues, whereClause, null)
            val executionTime = System.currentTimeMillis() - startTime

            QueryResult.mutation(rowsAffected, executionTime)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.error(e.message ?: "Update failed", executionTime)
        }
    }

    override suspend fun executeDelete(
        databaseName: String,
        tableName: String,
        whereClause: String
    ): QueryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val db = getDatabase(databaseName)
            val rowsAffected = db.delete(tableName, whereClause, null)
            val executionTime = System.currentTimeMillis() - startTime

            QueryResult.mutation(rowsAffected, executionTime)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            QueryResult.error(e.message ?: "Delete failed", executionTime)
        }
    }

    override suspend fun beginTransaction(databaseName: String) {
        val db = getDatabase(databaseName)
        activeTransactions[databaseName] = db
        db.beginTransaction()
    }

    override suspend fun commitTransaction(databaseName: String) {
        activeTransactions[databaseName]?.let { db ->
            db.setTransactionSuccessful()
            db.endTransaction()
            activeTransactions.remove(databaseName)
        }
    }

    override suspend fun rollbackTransaction(databaseName: String) {
        activeTransactions[databaseName]?.let { db ->
            db.endTransaction()
            activeTransactions.remove(databaseName)
        }
    }

    private fun getDatabase(name: String): SQLiteDatabase {
        return openDatabases.getOrPut(name) {
            val dbPath = databaseRepository.getDatabasePath(name)
            SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        }
    }

    private fun executeSelectQuery(db: SQLiteDatabase, sql: String): QueryResult {
        val cursor = db.rawQuery(sql, null)
        val rows = mutableListOf<Map<String, Any?>>()
        val columns = cursor.columnNames.toList()

        cursor.use { c ->
            while (c.moveToNext()) {
                val row = mutableMapOf<String, Any?>()
                columns.forEachIndexed { index, colName ->
                    row[colName] = getCursorValue(c, index)
                }
                rows.add(row)
            }
        }

        return QueryResult.success(rows, columns, 0)
    }

    private fun executeInsertQuery(db: SQLiteDatabase, sql: String): QueryResult {
        db.execSQL(sql)
        val cursor = db.rawQuery("SELECT changes()", null)
        val rowsAffected = cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return QueryResult.mutation(rowsAffected, 0)
    }

    private fun executeUpdateQuery(db: SQLiteDatabase, sql: String): QueryResult {
        db.execSQL(sql)
        val cursor = db.rawQuery("SELECT changes()", null)
        val rowsAffected = cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return QueryResult.mutation(rowsAffected, 0)
    }

    private fun executeDeleteQuery(db: SQLiteDatabase, sql: String): QueryResult {
        db.execSQL(sql)
        val cursor = db.rawQuery("SELECT changes()", null)
        val rowsAffected = cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return QueryResult.mutation(rowsAffected, 0)
    }

    private fun executeSchemaQuery(db: SQLiteDatabase, sql: String): QueryResult {
        db.execSQL(sql)
        return QueryResult.mutation(0, 0)
    }

    private fun executeGenericQuery(db: SQLiteDatabase, sql: String): QueryResult {
        db.execSQL(sql)
        return QueryResult.mutation(0, 0)
    }

    private fun getCursorValue(cursor: Cursor, index: Int): Any? {
        return when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
            Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
            Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(index)
            else -> cursor.getString(index)
        }
    }

    private suspend fun logQueryHistory(
        databaseName: String,
        query: String,
        executionTime: Long,
        result: QueryResult?,
        errorMessage: String? = null
    ) {
        try {
            val entity = QueryHistoryEntity(
                databaseName = databaseName,
                query = query,
                executionTimeMs = executionTime,
                rowCount = if (result?.isSelectQuery == true) result.rows.size else null,
                isSuccess = result?.isSuccess ?: false,
                errorMessage = errorMessage ?: result?.errorMessage
            )
            masterDb.queryHistoryDao().insert(entity)
            masterDb.queryHistoryDao().trimHistory(databaseName, 50)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log query history")
        }
    }

    fun closeAllDatabases() {
        openDatabases.values.forEach { it.close() }
        openDatabases.clear()
    }
}
