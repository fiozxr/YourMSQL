package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.ColumnInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnInfoDao {

    @Query("SELECT * FROM column_info WHERE tableId = :tableId ORDER BY columnOrder")
    fun getByTable(tableId: Long): Flow<List<ColumnInfoEntity>>

    @Query("SELECT * FROM column_info WHERE tableId = :tableId ORDER BY columnOrder")
    suspend fun getByTableSync(tableId: Long): List<ColumnInfoEntity>

    @Query("SELECT * FROM column_info WHERE tableId = :tableId AND name = :name LIMIT 1")
    suspend fun getByName(tableId: Long, name: String): ColumnInfoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(column: ColumnInfoEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(columns: List<ColumnInfoEntity>)

    @Update
    suspend fun update(column: ColumnInfoEntity)

    @Delete
    suspend fun delete(column: ColumnInfoEntity)

    @Query("DELETE FROM column_info WHERE tableId = :tableId AND name = :name")
    suspend fun deleteByName(tableId: Long, name: String)

    @Query("DELETE FROM column_info WHERE tableId = :tableId")
    suspend fun deleteByTable(tableId: Long)
}
