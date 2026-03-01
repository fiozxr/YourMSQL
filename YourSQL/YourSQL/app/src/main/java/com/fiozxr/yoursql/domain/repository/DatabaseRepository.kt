package com.fiozxr.yoursql.domain.repository

import com.fiozxr.yoursql.domain.model.DatabaseInfo
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    fun getAllDatabases(): Flow<List<DatabaseInfo>>
    suspend fun getDatabase(name: String): DatabaseInfo?
    suspend fun getActiveDatabase(): DatabaseInfo?
    fun getActiveDatabaseFlow(): Flow<DatabaseInfo?>
    suspend fun createDatabase(name: String, displayName: String? = null, description: String? = null): Result<DatabaseInfo>
    suspend fun deleteDatabase(name: String): Result<Unit>
    suspend fun setActiveDatabase(name: String): Result<Unit>
    suspend fun databaseExists(name: String): Boolean
    suspend fun getDatabasePath(name: String): String
    suspend fun exportDatabase(name: String, destinationPath: String): Result<String>
    suspend fun importDatabase(sourcePath: String, name: String): Result<DatabaseInfo>
    suspend fun getDatabaseSize(name: String): Long
}
