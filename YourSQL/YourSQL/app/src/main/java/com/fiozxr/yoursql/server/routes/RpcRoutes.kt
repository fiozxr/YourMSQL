package com.fiozxr.yoursql.server.routes

import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.domain.repository.DatabaseRepository
import com.fiozxr.yoursql.domain.repository.QueryExecutor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import timber.log.Timber

fun Route.rpcRoutes(
    queryExecutor: QueryExecutor,
    databaseRepository: DatabaseRepository
) {

    // POST /rest/v1/rpc/{function_name} - Execute a stored SQL function
    post("/rest/v1/rpc/{function_name}") {
        try {
            val functionName = call.parameters["function_name"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Function name required"))

            val databaseName = call.request.headers["x-database"] ?: "main"

            // Get function from database (or use built-in)
            val result = when (functionName) {
                "exec" -> {
                    // Execute raw SQL (admin only)
                    val body = call.receive<JsonObject>()
                    val sql = body["sql"]?.jsonPrimitive?.content
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "SQL required"))

                    queryExecutor.executeQuery(databaseName, sql)
                }

                "tables" -> {
                    // List all tables
                    queryExecutor.executeQuery(databaseName, "SELECT name FROM sqlite_master WHERE type='table'")
                }

                "table_info" -> {
                    // Get table info
                    val body = call.receive<JsonObject>()
                    val tableName = body["table"]?.jsonPrimitive?.content
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Table name required"))

                    queryExecutor.executeQuery(databaseName, "PRAGMA table_info($tableName)")
                }

                "indexes" -> {
                    // List indexes
                    val body = call.receive<JsonObject>()
                    val tableName = body["table"]?.jsonPrimitive?.content

                    val sql = if (tableName != null) {
                        "PRAGMA index_list($tableName)"
                    } else {
                        "SELECT name FROM sqlite_master WHERE type='index'"
                    }

                    queryExecutor.executeQuery(databaseName, sql)
                }

                "foreign_keys" -> {
                    // Get foreign keys
                    val body = call.receive<JsonObject>()
                    val tableName = body["table"]?.jsonPrimitive?.content
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Table name required"))

                    queryExecutor.executeQuery(databaseName, "PRAGMA foreign_key_list($tableName)")
                }

                "query_stats" -> {
                    // Get query statistics
                    queryExecutor.executeQuery(
                        databaseName,
                        "SELECT query, COUNT(*) as count, AVG(execution_time_ms) as avg_time " +
                                "FROM query_history GROUP BY query ORDER BY count DESC LIMIT 20"
                    )
                }

                else -> {
                    // Try to find custom function in database
                    // For now, return error
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Function '$functionName' not found")
                    )
                }
            }

            if (result.isSuccess) {
                if (result.isSelectQuery) {
                    call.respond(result.rows)
                } else {
                    call.respond(mapOf("rows_affected" to result.rowsAffected))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (result.errorMessage ?: "Query failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in RPC call")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
