package com.fiozxr.yoursql.server.routes

import com.fiozxr.yoursql.data.repository.ApiKeyRepositoryImpl
import com.fiozxr.yoursql.domain.model.ApiKeyScope
import com.fiozxr.yoursql.domain.model.ColumnType
import com.fiozxr.yoursql.domain.model.QueryResult
import com.fiozxr.yoursql.domain.repository.DatabaseRepository
import com.fiozxr.yoursql.domain.repository.QueryExecutor
import com.fiozxr.yoursql.domain.repository.TableRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import timber.log.Timber

fun Route.restRoutes(
    databaseRepository: DatabaseRepository,
    tableRepository: TableRepository,
    queryExecutor: QueryExecutor,
    apiKeyRepository: ApiKeyRepositoryImpl
) {

    // GET /rest/v1/{table} - List rows
    get("/rest/v1/{table}") {
        val tableName = call.parameters["table"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Table name required")
        )

        val databaseName = call.request.headers["x-database"] ?: getDefaultDatabase(databaseRepository)

        // Check permissions
        if (!hasReadPermission(call, apiKeyRepository)) {
            return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }

        try {
            // Parse query parameters
            val select = call.request.queryParameters["select"]
            val columns = select?.split(",")?.map { it.trim() }

            // Build WHERE clause from filter params
            val filters = buildFilterClause(call.request.queryParameters)

            // Order by
            val orderBy = call.request.queryParameters["order"]

            // Pagination
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
            val offset = call.request.queryParameters["offset"]?.toIntOrNull()

            // Execute query
            val result = queryExecutor.executeSelect(
                databaseName = databaseName,
                tableName = tableName,
                columns = columns,
                whereClause = filters,
                orderBy = orderBy,
                limit = limit,
                offset = offset
            )

            if (result.isSuccess) {
                // Handle Range header for pagination
                val rangeHeader = call.request.headers["Range"]
                if (rangeHeader != null) {
                    call.response.headers.append("Content-Range", "0-${result.rows.size - 1}/${result.rows.size}")
                }

                // Apply Prefer: count=exact if requested
                val prefer = call.request.headers["Prefer"]
                if (prefer?.contains("count=exact") == true) {
                    call.response.headers.append("X-Total-Count", result.rows.size.toString())
                }

                call.respond(result.rows)
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (result.errorMessage ?: "Query failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in GET /rest/v1/$tableName")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // POST /rest/v1/{table} - Insert rows
    post("/rest/v1/{table}") {
        val tableName = call.parameters["table"] ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Table name required")
        )

        val databaseName = call.request.headers["x-database"] ?: getDefaultDatabase(databaseRepository)

        // Check permissions
        if (!hasWritePermission(call, apiKeyRepository)) {
            return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }

        try {
            val body = call.receive<JsonElement>()
            val prefer = call.request.headers["Prefer"] ?: ""
            val returnRepresentation = prefer.contains("return=representation")

            val result = when (body) {
                is JsonObject -> {
                    val values = jsonObjectToMap(body)
                    queryExecutor.executeInsert(databaseName, tableName, values)
                }

                is JsonArray -> {
                    val valuesList = body.map { jsonObjectToMap(it as JsonObject) }
                    queryExecutor.executeBatchInsert(databaseName, tableName, valuesList)
                }

                else -> {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                }
            }

            if (result.isSuccess) {
                val statusCode = if (result.rowsAffected > 1) HttpStatusCode.Created else HttpStatusCode.Created

                if (returnRepresentation) {
                    call.respond(statusCode, result.rows)
                } else {
                    call.respond(statusCode, mapOf("rows_affected" to result.rowsAffected))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (result.errorMessage ?: "Insert failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in POST /rest/v1/$tableName")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // PATCH /rest/v1/{table} - Update rows
    patch("/rest/v1/{table}") {
        val tableName = call.parameters["table"] ?: return@patch call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Table name required")
        )

        val databaseName = call.request.headers["x-database"] ?: getDefaultDatabase(databaseRepository)

        // Check permissions
        if (!hasWritePermission(call, apiKeyRepository)) {
            return@patch call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }

        try {
            // Require at least one filter
            val filters = buildFilterClause(call.request.queryParameters)
            if (filters.isNullOrBlank()) {
                return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Filters required",
                        "hint" to "Add at least one filter parameter to prevent accidental updates"
                    )
                )
            }

            val body = call.receive<JsonObject>()
            val values = jsonObjectToMap(body)

            val result = queryExecutor.executeUpdate(databaseName, tableName, values, filters)

            if (result.isSuccess) {
                call.respond(mapOf("rows_affected" to result.rowsAffected))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (result.errorMessage ?: "Update failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in PATCH /rest/v1/$tableName")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // DELETE /rest/v1/{table} - Delete rows
    delete("/rest/v1/{table}") {
        val tableName = call.parameters["table"] ?: return@delete call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Table name required")
        )

        val databaseName = call.request.headers["x-database"] ?: getDefaultDatabase(databaseRepository)

        // Check permissions
        if (!hasWritePermission(call, apiKeyRepository)) {
            return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }

        try {
            // Require at least one filter
            val filters = buildFilterClause(call.request.queryParameters)
            if (filters.isNullOrBlank()) {
                return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Filters required",
                        "hint" to "Add at least one filter parameter to prevent accidental deletions"
                    )
                )
            }

            val result = queryExecutor.executeDelete(databaseName, tableName, filters)

            if (result.isSuccess) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (result.errorMessage ?: "Delete failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in DELETE /rest/v1/$tableName")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}

private suspend fun getDefaultDatabase(databaseRepository: DatabaseRepository): String {
    return databaseRepository.getActiveDatabase()?.name ?: "main"
}

private suspend fun hasReadPermission(call: ApplicationCall, apiKeyRepository: ApiKeyRepositoryImpl): Boolean {
    // Check if using JWT auth (authenticated users have read access)
    if (call.principal<JWTPrincipal>() != null) {
        return true
    }

    // Check API key scope
    val apiKey = extractApiKey(call) ?: return false
    val key = apiKeyRepository.getApiKey(apiKey) ?: return false

    return key.scope == ApiKeyScope.READ_ONLY ||
            key.scope == ApiKeyScope.READ_WRITE ||
            key.scope == ApiKeyScope.ADMIN
}

private suspend fun hasWritePermission(call: ApplicationCall, apiKeyRepository: ApiKeyRepositoryImpl): Boolean {
    // Check if using JWT auth
    val jwtPrincipal = call.principal<JWTPrincipal>()
    if (jwtPrincipal != null) {
        val role = jwtPrincipal.payload.getClaim("role")?.asString()
        return role == "ADMIN" || role == "admin"
    }

    // Check API key scope
    val apiKey = extractApiKey(call) ?: return false
    val key = apiKeyRepository.getApiKey(apiKey) ?: return false

    return key.scope == ApiKeyScope.READ_WRITE ||
            key.scope == ApiKeyScope.ADMIN
}

private fun extractApiKey(call: ApplicationCall): String? {
    var apiKey = call.request.headers["apikey"]
    if (apiKey == null) {
        val authHeader = call.request.headers[HttpHeaders.Authorization]
        if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            apiKey = authHeader.substring(7)
        }
    }
    return apiKey
}

private fun buildFilterClause(params: Parameters): String? {
    val conditions = mutableListOf<String>()

    // Supported operators
    val operators = mapOf(
        "eq" to "=",
        "neq" to "!=",
        "lt" to "<",
        "lte" to "<=",
        "gt" to ">",
        "gte" to ">=",
        "like" to "LIKE",
        "ilike" to "LIKE", // Case-insensitive (SQLite is case-insensitive by default for ASCII)
        "is" to "IS",
        "in" to "IN"
    )

    params.forEach { (key, values) ->
        // Skip non-filter params
        if (key in listOf("select", "order", "limit", "offset")) return@forEach

        val value = values.firstOrNull() ?: return@forEach

        // Parse operator from key (e.g., "name.eq" -> column="name", op="eq")
        val parts = key.split(".")
        val column = parts[0]
        val op = parts.getOrNull(1) ?: "eq"

        val sqlOp = operators[op] ?: "="

        val condition = when (op) {
            "in" -> "$column IN (${value.split(",").joinToString(", ") { "'$it'" }})"
            "is" -> "$column IS ${value.uppercase()}"
            else -> {
                val escapedValue = value.replace("'", "''")
                "$column $sqlOp '$escapedValue'"
            }
        }

        conditions.add(condition)
    }

    return if (conditions.isEmpty()) null else conditions.joinToString(" AND ")
}

private fun jsonObjectToMap(json: JsonObject): Map<String, Any?> {
    return json.mapValues { (_, value) ->
        when (value) {
            is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.boolean
                    value.longOrNull != null -> value.long
                    value.doubleOrNull != null -> value.double
                    else -> value.content
                }
            }

            else -> value.toString()
        }
    }
}
