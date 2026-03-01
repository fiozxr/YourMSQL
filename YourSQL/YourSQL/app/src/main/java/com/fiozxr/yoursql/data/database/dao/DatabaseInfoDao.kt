package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.DatabaseInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseInfoDao {

    @Query("SELECT * FROM database_info ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DatabaseInfoEntity>>

    @Query("SELECT * FROM database_info WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): DatabaseInfoEntity?

    @Query("SELECT * FROM database_info WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): DatabaseInfoEntity?

    @Query("SELECT * FROM database_info WHERE isActive = 1 LIMIT 1")
    fun getActiveFlow(): Flow<DatabaseInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(database: DatabaseInfoEntity)

    @Update
    suspend fun update(database: DatabaseInfoEntity)

    @Query("UPDATE database_info SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActive()

    @Query("UPDATE database_info SET isActive = 1 WHERE name = :name")
    suspend fun setActive(name: String)

    @Delete
    suspend fun delete(database: DatabaseInfoEntity)

    @Query("DELETE FROM database_info WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT(*) FROM database_info")
    suspend fun count(): Int
}
