package com.fiozxr.yoursql.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.DatabaseInfoEntity
import com.fiozxr.yoursql.domain.model.DatabaseInfo
import com.fiozxr.yoursql.domain.repository.DatabaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterDb: MasterDatabase
) : DatabaseRepository {

    private val dao = masterDb.databaseInfoDao()

    override fun getAllDatabases(): Flow<List<DatabaseInfo>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getDatabase(name: String): DatabaseInfo? {
        return dao.getByName(name)?.toDomainModel()
    }

    override suspend fun getActiveDatabase(): DatabaseInfo? {
        return dao.getActive()?.toDomainModel()
    }

    override fun getActiveDatabaseFlow(): Flow<DatabaseInfo?> {
        return dao.getActiveFlow().map { it?.toDomainModel() }
    }

    override suspend fun createDatabase(
        name: String,
        displayName: String?,
        description: String?
    ): Result<DatabaseInfo> = withContext(Dispatchers.IO) {
        try {
            // Validate name
            if (!name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                return@withContext Result.failure(
                    IllegalArgumentException("Database name must start with a letter and contain only letters, numbers, and underscores")
                )
            }

            // Check if database already exists
            if (dao.getByName(name) != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Database '$name' already exists")
                )
            }

            // Create the actual SQLite database file
            val dbFile = getDatabaseFile(name)
            if (dbFile.exists()) {
                dbFile.delete()
            }

            // Open and close to create the file
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null).close()

            // Insert metadata
            val entity = DatabaseInfoEntity(
                name = name,
                displayName = displayName ?: name,
                description = description
            )
            dao.insert(entity)

            // If this is the first database, make it active
            if (dao.count() == 1) {
                dao.setActive(name)
            }

            Timber.d("Created database: $name")
            Result.success(entity.toDomainModel())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create database: $name")
            Result.failure(e)
        }
    }

    override suspend fun deleteDatabase(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getByName(name)
                ?: return@withContext Result.failure(IllegalArgumentException("Database '$name' not found"))

            // Delete the actual database file
            val dbFile = getDatabaseFile(name)
            if (dbFile.exists()) {
                dbFile.delete()
            }

            // Delete journal files if they exist
            val journalFile = File(dbFile.parent, "$name-journal")
            val walFile = File(dbFile.parent, "$name-wal")
            val shmFile = File(dbFile.parent, "$name-shm")
            journalFile.delete()
            walFile.delete()
            shmFile.delete()

            // Delete metadata
            dao.delete(entity)

            // If this was the active database, clear active
            if (entity.isActive) {
                // Set another database as active if available
                val remaining = dao.getAll()
                remaining.collect { list ->
                    list.firstOrNull()?.let {
                        dao.setActive(it.name)
                    }
                }
            }

            Timber.d("Deleted database: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete database: $name")
            Result.failure(e)
        }
    }

    override suspend fun setActiveDatabase(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (dao.getByName(name) == null) {
                return@withContext Result.failure(IllegalArgumentException("Database '$name' not found"))
            }

            dao.clearActive()
            dao.setActive(name)

            Timber.d("Set active database: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set active database: $name")
            Result.failure(e)
        }
    }

    override suspend fun databaseExists(name: String): Boolean {
        return dao.getByName(name) != null && getDatabaseFile(name).exists()
    }

    override suspend fun getDatabasePath(name: String): String {
        return getDatabaseFile(name).absolutePath
    }

    override suspend fun exportDatabase(name: String, destinationPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = getDatabaseFile(name)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(IllegalArgumentException("Database '$name' not found"))
                }

                val destFile = File(destinationPath)
                destFile.parentFile?.mkdirs()

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Timber.d("Exported database $name to $destinationPath")
                Result.success(destFile.absolutePath)
            } catch (e: Exception) {
                Timber.e(e, "Failed to export database: $name")
                Result.failure(e)
            }
        }

    override suspend fun importDatabase(sourcePath: String, name: String): Result<DatabaseInfo> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(IllegalArgumentException("Source file not found: $sourcePath"))
                }

                // Delete existing database if it exists
                if (dao.getByName(name) != null) {
                    deleteDatabase(name)
                }

                // Copy the file
                val destFile = getDatabaseFile(name)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Create metadata
                val entity = DatabaseInfoEntity(
                    name = name,
                    displayName = name
                )
                dao.insert(entity)

                Timber.d("Imported database $name from $sourcePath")
                Result.success(entity.toDomainModel())
            } catch (e: Exception) {
                Timber.e(e, "Failed to import database: $name")
                Result.failure(e)
            }
        }

    override suspend fun getDatabaseSize(name: String): Long = withContext(Dispatchers.IO) {
        val dbFile = getDatabaseFile(name)
        if (dbFile.exists()) dbFile.length() else 0L
    }

    private fun getDatabaseFile(name: String): File {
        return File(context.getDatabasePath(name).absolutePath)
    }

    private fun DatabaseInfoEntity.toDomainModel(): DatabaseInfo {
        return DatabaseInfo(
            name = name,
            displayName = displayName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isActive = isActive,
            description = description
        )
    }
}
