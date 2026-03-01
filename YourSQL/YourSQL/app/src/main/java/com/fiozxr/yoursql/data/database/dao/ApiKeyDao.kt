package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.ApiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {

    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(apiKey: ApiKeyEntity)

    @Update
    suspend fun update(apiKey: ApiKeyEntity)

    @Query("UPDATE api_keys SET requestCount = requestCount + 1, lastUsedAt = :timestamp WHERE key = :key")
    suspend fun incrementRequestCount(key: String, timestamp: Long)

    @Delete
    suspend fun delete(apiKey: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT COUNT(*) FROM api_keys WHERE isActive = 1")
    suspend fun countActive(): Int
}
