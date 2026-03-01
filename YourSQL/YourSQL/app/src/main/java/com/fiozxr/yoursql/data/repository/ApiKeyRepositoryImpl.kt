package com.fiozxr.yoursql.data.repository

import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.ApiKeyEntity
import com.fiozxr.yoursql.domain.model.ApiKey
import com.fiozxr.yoursql.domain.model.ApiKeyScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepositoryImpl @Inject constructor(
    private val masterDb: MasterDatabase
) {
    private val dao = masterDb.apiKeyDao()

    fun getAllApiKeys(): Flow<List<ApiKey>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getAllApiKeysSync(): List<ApiKey> {
        return dao.getAllSync().map { it.toDomainModel() }
    }

    suspend fun getApiKey(key: String): ApiKey? {
        return dao.getByKey(key)?.toDomainModel()
    }

    suspend fun getDefaultApiKey(): ApiKey? {
        return dao.getDefault()?.toDomainModel()
    }

    suspend fun createApiKey(name: String, scope: ApiKeyScope, rateLimit: Int = 100): Result<ApiKey> =
        withContext(Dispatchers.Default) {
            try {
                val apiKey = ApiKey.generate(name, scope).copy(rateLimitPerMinute = rateLimit)
                dao.insert(apiKey.toEntity())
                Timber.d("Created API key: $name with scope $scope")
                Result.success(apiKey)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create API key")
                Result.failure(e)
            }
        }

    suspend fun createDefaultApiKey(): Result<ApiKey> =
        withContext(Dispatchers.Default) {
            try {
                // Check if default key already exists
                dao.getDefault()?.let {
                    return@withContext Result.success(it.toDomainModel())
                }

                val apiKey = ApiKey.generate("Anonymous Key", ApiKeyScope.READ_WRITE).copy(isDefault = true)
                dao.insert(apiKey.toEntity())
                Timber.d("Created default API key")
                Result.success(apiKey)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create default API key")
                Result.failure(e)
            }
        }

    suspend fun revokeApiKey(key: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dao.deleteByKey(key)
                Timber.d("Revoked API key: $key")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to revoke API key")
                Result.failure(e)
            }
        }

    suspend fun updateApiKey(apiKey: ApiKey): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dao.update(apiKey.toEntity())
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update API key")
                Result.failure(e)
            }
        }

    suspend fun incrementRequestCount(key: String) {
        dao.incrementRequestCount(key, Instant.now().epochSecond)
    }

    private fun ApiKeyEntity.toDomainModel(): ApiKey {
        return ApiKey(
            key = key,
            name = name,
            scope = ApiKeyScope.fromEntityString(scope),
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
            requestCount = requestCount,
            rateLimitPerMinute = rateLimitPerMinute,
            isActive = isActive,
            isDefault = isDefault
        )
    }

    private fun ApiKey.toEntity(): ApiKeyEntity {
        return ApiKeyEntity(
            key = key,
            name = name,
            scope = scope.toEntityString(),
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
            requestCount = requestCount,
            rateLimitPerMinute = rateLimitPerMinute,
            isActive = isActive,
            isDefault = isDefault
        )
    }
}
