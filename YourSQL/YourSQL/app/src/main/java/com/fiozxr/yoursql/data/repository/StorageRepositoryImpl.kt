package com.fiozxr.yoursql.data.repository

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.BucketEntity
import com.fiozxr.yoursql.data.database.entity.StorageFileEntity
import com.fiozxr.yoursql.domain.model.Bucket
import com.fiozxr.yoursql.domain.model.StorageFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterDb: MasterDatabase
) {
    private val bucketDao = masterDb.bucketDao()
    private val fileDao = masterDb.storageFileDao()

    private val storageDir: File
        get() = File(context.getExternalFilesDir(null), "storage").apply { mkdirs() }

    fun getAllBuckets(): Flow<List<Bucket>> {
        return bucketDao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getAllBucketsSync(): List<Bucket> {
        return bucketDao.getAllSync().map { it.toDomainModel() }
    }

    suspend fun getBucket(name: String): Bucket? {
        return bucketDao.getByName(name)?.toDomainModel()
    }

    suspend fun createBucket(name: String, isPublic: Boolean = false): Result<Bucket> =
        withContext(Dispatchers.IO) {
            try {
                if (!name.matches(Regex("^[a-z0-9-]+$"))) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Bucket name must contain only lowercase letters, numbers, and hyphens")
                    )
                }

                if (bucketDao.getByName(name) != null) {
                    return@withContext Result.failure(IllegalArgumentException("Bucket '$name' already exists"))
                }

                // Create directory
                val bucketDir = File(storageDir, name)
                bucketDir.mkdirs()

                // Insert metadata
                val entity = BucketEntity(name = name, isPublic = isPublic)
                bucketDao.insert(entity)

                Timber.d("Created bucket: $name")
                Result.success(entity.toDomainModel())
            } catch (e: Exception) {
                Timber.e(e, "Failed to create bucket")
                Result.failure(e)
            }
        }

    suspend fun deleteBucket(name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Delete all files
                fileDao.deleteByBucket(name)

                // Delete directory
                val bucketDir = File(storageDir, name)
                bucketDir.deleteRecursively()

                // Delete metadata
                bucketDao.deleteByName(name)

                Timber.d("Deleted bucket: $name")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete bucket")
                Result.failure(e)
            }
        }

    fun getFilesInBucket(bucketName: String): Flow<List<StorageFile>> {
        return fileDao.getByBucket(bucketName).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun uploadFile(
        bucketName: String,
        path: String,
        inputStream: InputStream,
        size: Long,
        mimeType: String? = null
    ): Result<StorageFile> = withContext(Dispatchers.IO) {
        try {
            val bucket = bucketDao.getByName(bucketName)
                ?: return@withContext Result.failure(IllegalArgumentException("Bucket not found: $bucketName"))

            // Check storage quota
            val totalUsed = bucketDao.getTotalStorageUsed() ?: 0L
            val quota = getStorageQuota()
            if (totalUsed + size > quota) {
                return@withContext Result.failure(IllegalStateException("Storage quota exceeded"))
            }

            // Sanitize path
            val sanitizedPath = path.trim('/').replace("..", "")
            val fileName = sanitizedPath.substringAfterLast('/')

            // Create directories if needed
            val bucketDir = File(storageDir, bucketName)
            val fileDir = if (sanitizedPath.contains('/')) {
                File(bucketDir, sanitizedPath.substringBeforeLast('/')).apply { mkdirs() }
            } else {
                bucketDir
            }

            // Save file
            val file = File(fileDir, fileName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }

            // Detect MIME type
            val detectedMimeType = mimeType ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension) ?: "application/octet-stream"

            // Insert metadata
            val fileId = UUID.randomUUID().toString()
            val entity = StorageFileEntity(
                id = fileId,
                bucketName = bucketName,
                path = sanitizedPath,
                name = fileName,
                size = size,
                mimeType = detectedMimeType
            )
            fileDao.insert(entity)

            // Update bucket stats
            updateBucketStats(bucketName)

            Timber.d("Uploaded file: $sanitizedPath to bucket $bucketName")
            Result.success(entity.toDomainModel())
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload file")
            Result.failure(e)
        }
    }

    suspend fun getFile(bucketName: String, path: String): Pair<StorageFile, File>? =
        withContext(Dispatchers.IO) {
            try {
                val entity = fileDao.getByPath(bucketName, path.trim('/'))
                    ?: return@withContext null

                val bucketDir = File(storageDir, bucketName)
                val file = if (path.contains('/')) {
                    File(bucketDir, path.trim('/'))
                } else {
                    File(bucketDir, entity.name)
                }

                if (!file.exists()) {
                    return@withContext null
                }

                Pair(entity.toDomainModel(), file)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get file")
                null
            }
        }

    suspend fun deleteFile(bucketName: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = fileDao.getByPath(bucketName, path.trim('/'))
                    ?: return@withContext Result.failure(IllegalArgumentException("File not found"))

                // Delete physical file
                val bucketDir = File(storageDir, bucketName)
                val file = File(bucketDir, path.trim('/'))
                file.delete()

                // Delete metadata
                fileDao.delete(entity)

                // Update bucket stats
                updateBucketStats(bucketName)

                Timber.d("Deleted file: $path from bucket $bucketName")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete file")
                Result.failure(e)
            }
        }

    suspend fun getTotalStorageUsed(): Long =
        withContext(Dispatchers.IO) {
            bucketDao.getTotalStorageUsed() ?: 0L
        }

    suspend fun getStorageQuota(): Long {
        // Default 1GB, could be loaded from preferences
        return 1L * 1024 * 1024 * 1024
    }

    private suspend fun updateBucketStats(bucketName: String) {
        val totalSize = fileDao.getBucketSize(bucketName) ?: 0L
        val fileCount = fileDao.getBucketFileCount(bucketName)
        bucketDao.updateStats(bucketName, totalSize, fileCount)
    }

    private fun BucketEntity.toDomainModel(): Bucket {
        return Bucket(
            name = name,
            isPublic = isPublic,
            createdAt = createdAt,
            totalSize = totalSize,
            fileCount = fileCount
        )
    }

    private fun StorageFileEntity.toDomainModel(): StorageFile {
        return StorageFile(
            id = id,
            bucketName = bucketName,
            path = path,
            name = name,
            size = size,
            mimeType = mimeType,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
