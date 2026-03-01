package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.BucketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {

    @Query("SELECT * FROM buckets ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BucketEntity>>

    @Query("SELECT * FROM buckets ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<BucketEntity>

    @Query("SELECT * FROM buckets WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): BucketEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bucket: BucketEntity)

    @Update
    suspend fun update(bucket: BucketEntity)

    @Query("UPDATE buckets SET totalSize = :totalSize, fileCount = :fileCount WHERE name = :name")
    suspend fun updateStats(name: String, totalSize: Long, fileCount: Int)

    @Delete
    suspend fun delete(bucket: BucketEntity)

    @Query("DELETE FROM buckets WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT SUM(totalSize) FROM buckets")
    suspend fun getTotalStorageUsed(): Long?
}
