package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.QueryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryHistoryDao {

    @Query("SELECT * FROM query_history WHERE databaseName = :databaseName ORDER BY executedAt DESC LIMIT :limit")
    fun getRecent(databaseName: String, limit: Int = 50): Flow<List<QueryHistoryEntity>>

    @Query("SELECT * FROM query_history WHERE databaseName = :databaseName ORDER BY executedAt DESC LIMIT :limit")
    suspend fun getRecentSync(databaseName: String, limit: Int = 50): List<QueryHistoryEntity>

    @Insert
    suspend fun insert(entry: QueryHistoryEntity): Long

    @Query("DELETE FROM query_history WHERE databaseName = :databaseName AND id NOT IN (SELECT id FROM query_history WHERE databaseName = :databaseName ORDER BY executedAt DESC LIMIT :keepCount)")
    suspend fun trimHistory(databaseName: String, keepCount: Int = 50)

    @Query("DELETE FROM query_history WHERE databaseName = :databaseName")
    suspend fun clearHistory(databaseName: String)
}
