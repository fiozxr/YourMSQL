package com.fiozxr.yoursql.data.repository

import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.database.entity.UserEntity
import com.fiozxr.yoursql.domain.model.User
import com.fiozxr.yoursql.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val masterDb: MasterDatabase
) {
    private val dao = masterDb.userDao()

    fun getAllUsers(): Flow<List<User>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getUserById(id: String): User? {
        return dao.getById(id)?.toDomainModel()
    }

    suspend fun getUserByEmail(email: String): User? {
        return dao.getByEmail(email)?.toDomainModel()
    }

    suspend fun createUser(email: String, password: String, role: UserRole = UserRole.USER): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // Check if user already exists
                if (dao.getByEmail(email) != null) {
                    return@withContext Result.failure(IllegalArgumentException("User with email $email already exists"))
                }

                val (user, passwordHash) = User.create(email, password, role)
                dao.insert(user.toEntity(passwordHash))

                Timber.d("Created user: $email")
                Result.success(user)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create user")
                Result.failure(e)
            }
        }

    suspend fun authenticateUser(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val entity = dao.getByEmail(email)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid credentials"))

                if (!User.verifyPassword(password, entity.passwordHash)) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid credentials"))
                }

                if (!entity.isActive) {
                    return@withContext Result.failure(IllegalArgumentException("Account is deactivated"))
                }

                // Update last login
                dao.updateLastLogin(entity.id, Instant.now().epochSecond)

                Result.success(entity.toDomainModel())
            } catch (e: Exception) {
                Timber.e(e, "Failed to authenticate user")
                Result.failure(e)
            }
        }

    suspend fun updateUser(user: User): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val existing = dao.getById(user.id)
                    ?: return@withContext Result.failure(IllegalArgumentException("User not found"))

                dao.update(user.toEntity(existing.passwordHash))
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user")
                Result.failure(e)
            }
        }

    suspend fun changePassword(userId: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val existing = dao.getById(userId)
                    ?: return@withContext Result.failure(IllegalArgumentException("User not found"))

                val newPasswordHash = User.hashPassword(newPassword)
                dao.update(existing.copy(passwordHash = newPasswordHash))
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to change password")
                Result.failure(e)
            }
        }

    suspend fun deleteUser(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dao.deleteById(userId)
                Timber.d("Deleted user: $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete user")
                Result.failure(e)
            }
        }

    private fun UserEntity.toDomainModel(): User {
        return User(
            id = id,
            email = email,
            role = UserRole.fromEntityString(role),
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }

    private fun User.toEntity(passwordHash: String): UserEntity {
        return UserEntity(
            id = id,
            email = email,
            passwordHash = passwordHash,
            role = role.toEntityString(),
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }
}
