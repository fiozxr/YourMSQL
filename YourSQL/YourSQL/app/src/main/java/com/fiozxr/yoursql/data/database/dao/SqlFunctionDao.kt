package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.SqlFunctionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SqlFunctionDao {

    @Query("SELECT * FROM sql_functions ORDER BY name")
    fun getAll(): Flow<List<SqlFunctionEntity>>

    @Query("SELECT * FROM sql_functions ORDER BY name")
    suspend fun getAllSync(): List<SqlFunctionEntity>

    @Query("SELECT * FROM sql_functions WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SqlFunctionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(function: SqlFunctionEntity)

    @Update
    suspend fun update(function: SqlFunctionEntity)

    @Delete
    suspend fun delete(function: SqlFunctionEntity)

    @Query("DELETE FROM sql_functions WHERE name = :name")
    suspend fun deleteByName(name: String)
}
