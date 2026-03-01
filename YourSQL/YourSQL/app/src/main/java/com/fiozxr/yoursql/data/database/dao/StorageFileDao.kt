package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.StorageFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageFileDao {

    @Query("SELECT * FROM storage_files WHERE bucketName = :bucketName ORDER BY path")
    fun getByBucket(bucketName: String): Flow<List<StorageFileEntity>>

    @Query("SELECT * FROM storage_files WHERE bucketName = :bucketName ORDER BY path")
    suspend fun getByBucketSync(bucketName: String): List<StorageFileEntity>

    @Query("SELECT * FROM storage_files WHERE bucketName = :bucketName AND path = :path LIMIT 1")
    suspend fun getByPath(bucketName: String, path: String): StorageFileEntity?

    @Query("SELECT * FROM storage_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StorageFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: StorageFileEntity)

    @Update
    suspend fun update(file: StorageFileEntity)

    @Delete
    suspend fun delete(file: StorageFileEntity)

    @Query("DELETE FROM storage_files WHERE bucketName = :bucketName AND path = :path")
    suspend fun deleteByPath(bucketName: String, path: String)

    @Query("DELETE FROM storage_files WHERE bucketName = :bucketName")
    suspend fun deleteByBucket(bucketName: String)

    @Query("SELECT SUM(size) FROM storage_files WHERE bucketName = :bucketName")
    suspend fun getBucketSize(bucketName: String): Long?

    @Query("SELECT COUNT(*) FROM storage_files WHERE bucketName = :bucketName")
    suspend fun getBucketFileCount(bucketName: String): Int
}
