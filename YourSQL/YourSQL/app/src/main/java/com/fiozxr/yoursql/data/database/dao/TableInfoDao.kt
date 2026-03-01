package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.TableInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableInfoDao {

    @Query("SELECT * FROM table_info WHERE databaseName = :databaseName ORDER BY name")
    fun getByDatabase(databaseName: String): Flow<List<TableInfoEntity>>

    @Query("SELECT * FROM table_info WHERE databaseName = :databaseName ORDER BY name")
    suspend fun getByDatabaseSync(databaseName: String): List<TableInfoEntity>

    @Query("SELECT * FROM table_info WHERE databaseName = :databaseName AND name = :name LIMIT 1")
    suspend fun getByName(databaseName: String, name: String): TableInfoEntity?

    @Query("SELECT * FROM table_info WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TableInfoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(table: TableInfoEntity): Long

    @Update
    suspend fun update(table: TableInfoEntity)

    @Query("UPDATE table_info SET rowCount = :rowCount, updatedAt = :timestamp WHERE id = :tableId")
    suspend fun updateRowCount(tableId: Long, rowCount: Long, timestamp: Long)

    @Delete
    suspend fun delete(table: TableInfoEntity)

    @Query("DELETE FROM table_info WHERE databaseName = :databaseName AND name = :name")
    suspend fun deleteByName(databaseName: String, name: String)

    @Query("DELETE FROM table_info WHERE databaseName = :databaseName")
    suspend fun deleteByDatabase(databaseName: String)
}
