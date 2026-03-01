package com.fiozxr.yoursql.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.ColumnInfoEntity
import com.fiozxr.yoursql.data.database.entity.TableInfoEntity
import com.fiozxr.yoursql.domain.model.ColumnInfo
import com.fiozxr.yoursql.domain.model.ColumnType
import com.fiozxr.yoursql.domain.model.TableInfo
import com.fiozxr.yoursql.domain.repository.TableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableRepositoryImpl @Inject constructor(
    private val masterDb: MasterDatabase,
    private val databaseRepository: DatabaseRepositoryImpl
) : TableRepository {

    private val tableDao = masterDb.tableInfoDao()
    private val columnDao = masterDb.columnInfoDao()

    override fun getTables(databaseName: String): Flow<List<TableInfo>> {
        return tableDao.getByDatabase(databaseName).map { entities ->
            entities.map { entity ->
                val columns = columnDao.getByTableSync(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(columns)
            }
        }
    }

    override suspend fun getTable(databaseName: String, tableName: String): TableInfo? {
        val entity = tableDao.getByName(databaseName, tableName) ?: return null
        val columns = columnDao.getByTableSync(entity.id).map { it.toDomainModel() }
        return entity.toDomainModel(columns)
    }

    override suspend fun getTableById(tableId: Long): TableInfo? {
        val entity = tableDao.getById(tableId) ?: return null
        val columns = columnDao.getByTableSync(entity.id).map { it.toDomainModel() }
        return entity.toDomainModel(columns)
    }

    override suspend fun createTable(
        databaseName: String,
        tableName: String,
        columns: List<ColumnInfo>
    ): Result<TableInfo> = withContext(Dispatchers.IO) {
        try {
            // Validate table name
            if (!tableName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                return@withContext Result.failure(
                    IllegalArgumentException("Table name must start with a letter and contain only letters, numbers, and underscores")
                )
            }

            // Validate columns
            if (columns.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Table must have at least one column")
                )
            }

            val hasPrimaryKey = columns.any { it.isPrimaryKey }
            if (!hasPrimaryKey) {
                return@withContext Result.failure(
                    IllegalArgumentException("Table must have a primary key column")
                )
            }

            // Get database
            val dbPath = databaseRepository.getDatabasePath(databaseName)
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

            db.use { database ->
                // Create table SQL
                val columnsSql = columns.joinToString(", ") { col ->
                    val sb = StringBuilder()
                    sb.append("${col.name} ${col.type.sqlName}")
                    if (col.isPrimaryKey) {
                        sb.append(" PRIMARY KEY AUTOINCREMENT")
                    }
                    if (!col.isNullable && !col.isPrimaryKey) {
                        sb.append(" NOT NULL")
                    }
                    col.defaultValue?.let { sb.append(" DEFAULT $it") }
                    sb.toString()
                }

                val createSql = "CREATE TABLE IF NOT EXISTS $tableName ($columnsSql)"
                database.execSQL(createSql)
            }

            // Insert metadata
            val tableEntity = TableInfoEntity(
                databaseName = databaseName,
                name = tableName,
                displayName = tableName
            )
            val tableId = tableDao.insert(tableEntity)

            // Insert column metadata
            val columnEntities = columns.mapIndexed { index, col ->
                ColumnInfoEntity(
                    tableId = tableId,
                    name = col.name,
                    type = col.type.sqlName,
                    isNullable = col.isNullable,
                    isPrimaryKey = col.isPrimaryKey,
                    defaultValue = col.defaultValue,
                    columnOrder = index
                )
            }
            columnDao.insertAll(columnEntities)

            // Return created table info
            val createdTable = getTable(databaseName, tableName)
                ?: return@withContext Result.failure(IllegalStateException("Failed to retrieve created table"))

            Timber.d("Created table $tableName in database $databaseName")
            Result.success(createdTable)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create table $tableName")
            Result.failure(e)
        }
    }

    override suspend fun dropTable(databaseName: String, tableName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val dbPath = databaseRepository.getDatabasePath(databaseName)
                val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

                db.use { database ->
                    database.execSQL("DROP TABLE IF EXISTS $tableName")
                }

                // Delete metadata
                tableDao.deleteByName(databaseName, tableName)

                Timber.d("Dropped table $tableName from database $databaseName")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to drop table $tableName")
                Result.failure(e)
            }
        }

    override suspend fun renameTable(
        databaseName: String,
        oldName: String,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!newName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid table name: $newName")
                )
            }

            val dbPath = databaseRepository.getDatabasePath(databaseName)
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

            db.use { database ->
                database.execSQL("ALTER TABLE $oldName RENAME TO $newName")
            }

            // Update metadata
            val table = tableDao.getByName(databaseName, oldName)
            table?.let {
                tableDao.update(it.copy(name = newName, displayName = newName))
            }

            Timber.d("Renamed table $oldName to $newName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename table $oldName")
            Result.failure(e)
        }
    }

    override suspend fun addColumn(
        databaseName: String,
        tableName: String,
        column: ColumnInfo
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbPath = databaseRepository.getDatabasePath(databaseName)
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

            val alterSql = buildString {
                append("ALTER TABLE $tableName ADD COLUMN ${column.name} ${column.type.sqlName}")
                if (!column.isNullable) {
                    append(" NOT NULL DEFAULT ${column.defaultValue ?: "''"}")
                }
            }

            db.use { database ->
                database.execSQL(alterSql)
            }

            // Update metadata
            val table = tableDao.getByName(databaseName, tableName)
            table?.let { t ->
                val existingColumns = columnDao.getByTableSync(t.id)
                val newColumnEntity = ColumnInfoEntity(
                    tableId = t.id,
                    name = column.name,
                    type = column.type.sqlName,
                    isNullable = column.isNullable,
                    isPrimaryKey = column.isPrimaryKey,
                    defaultValue = column.defaultValue,
                    columnOrder = existingColumns.size
                )
                columnDao.insert(newColumnEntity)
                tableDao.update(t.copy(updatedAt = Instant.now().epochSecond))
            }

            Timber.d("Added column ${column.name} to table $tableName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add column to table $tableName")
            Result.failure(e)
        }
    }

    override suspend fun dropColumn(
        databaseName: String,
        tableName: String,
        columnName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // SQLite doesn't support DROP COLUMN directly before version 3.35.0
            // We need to recreate the table without the column
            val dbPath = databaseRepository.getDatabasePath(databaseName)
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

            db.use { database ->
                // Get existing columns
                val cursor = database.rawQuery("PRAGMA table_info($tableName)", null)
                val columns = mutableListOf<Triple<String, String, Boolean>>()
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val name = c.getString(c.getColumnIndexOrThrow("name"))
                        val type = c.getString(c.getColumnIndexOrThrow("type"))
                        val notNull = c.getInt(c.getColumnIndexOrThrow("notnull")) == 1
                        if (name != columnName) {
                            columns.add(Triple(name, type, notNull))
                        }
                    }
                }

                // Create new table without the column
                val tempTableName = "${tableName}_temp"
                val columnsSql = columns.joinToString(", ") { (name, type, notNull) ->
                    "$name $type${if (notNull) " NOT NULL" else ""}"
                }

                database.execSQL("CREATE TABLE $tempTableName ($columnsSql)")
                database.execSQL("INSERT INTO $tempTableName SELECT ${columns.joinToString(", ") { it.first }} FROM $tableName")
                database.execSQL("DROP TABLE $tableName")
                database.execSQL("ALTER TABLE $tempTableName RENAME TO $tableName")
            }

            // Update metadata
            val table = tableDao.getByName(databaseName, tableName)
            table?.let { t ->
                val column = columnDao.getByName(t.id, columnName)
                column?.let { columnDao.delete(it) }
                tableDao.update(t.copy(updatedAt = Instant.now().epochSecond))
            }

            Timber.d("Dropped column $columnName from table $tableName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to drop column from table $tableName")
            Result.failure(e)
        }
    }

    override suspend fun getTableRowCount(databaseName: String, tableName: String): Long =
        withContext(Dispatchers.IO) {
            try {
                val dbPath = databaseRepository.getDatabasePath(databaseName)
                val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

                db.use { database ->
                    val cursor = database.rawQuery("SELECT COUNT(*) FROM $tableName", null)
                    cursor.use { c ->
                        if (c.moveToFirst()) {
                            c.getLong(0)
                        } else {
                            0L
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get row count for table $tableName")
                0L
            }
        }

    override suspend fun refreshTableMetadata(databaseName: String, tableName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                syncSchemaFromDatabase(databaseName)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun syncSchemaFromDatabase(databaseName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val dbPath = databaseRepository.getDatabasePath(databaseName)
                val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

                db.use { database ->
                    // Get all tables
                    val cursor = database.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
                        null
                    )

                    val tableNames = mutableListOf<String>()
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            tableNames.add(c.getString(0))
                        }
                    }

                    // Sync each table
                    for (tableName in tableNames) {
                        syncTableSchema(database, databaseName, tableName)
                    }
                }

                Timber.d("Synced schema from database $databaseName")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync schema from database $databaseName")
                Result.failure(e)
            }
        }

    private suspend fun syncTableSchema(
        db: SQLiteDatabase,
        databaseName: String,
        tableName: String
    ) {
        // Get or create table metadata
        var tableEntity = tableDao.getByName(databaseName, tableName)
        if (tableEntity == null) {
            val tableId = tableDao.insert(
                TableInfoEntity(
                    databaseName = databaseName,
                    name = tableName,
                    displayName = tableName
                )
            )
            tableEntity = tableDao.getById(tableId)
        }

        tableEntity?.let { table ->
            // Get column info from database
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            val dbColumns = mutableListOf<ColumnInfoEntity>()

            cursor.use { c ->
                var order = 0
                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndexOrThrow("name"))
                    val type = c.getString(c.getColumnIndexOrThrow("type"))
                    val notNull = c.getInt(c.getColumnIndexOrThrow("notnull")) == 1
                    val pk = c.getInt(c.getColumnIndexOrThrow("pk")) == 1
                    val defaultValue = c.getString(c.getColumnIndexOrThrow("dflt_value"))

                    dbColumns.add(
                        ColumnInfoEntity(
                            tableId = table.id,
                            name = name,
                            type = type,
                            isNullable = !notNull,
                            isPrimaryKey = pk,
                            defaultValue = defaultValue,
                            columnOrder = order++
                        )
                    )
                }
            }

            // Delete existing columns and insert new ones
            columnDao.deleteByTable(table.id)
            columnDao.insertAll(dbColumns)

            // Update row count
            val rowCountCursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            val rowCount = rowCountCursor.use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
            tableDao.updateRowCount(table.id, rowCount, Instant.now().epochSecond)
        }
    }

    private fun TableInfoEntity.toDomainModel(columns: List<ColumnInfo> = emptyList()): TableInfo {
        return TableInfo(
            id = id,
            databaseName = databaseName,
            name = name,
            displayName = displayName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            rowCount = rowCount,
            columns = columns
        )
    }

    private fun ColumnInfoEntity.toDomainModel(): ColumnInfo {
        return ColumnInfo(
            id = id,
            tableId = tableId,
            name = name,
            type = ColumnType.fromSqlName(type) ?: ColumnType.TEXT,
            isNullable = isNullable,
            isPrimaryKey = isPrimaryKey,
            defaultValue = defaultValue,
            columnOrder = columnOrder
        )
    }
}
