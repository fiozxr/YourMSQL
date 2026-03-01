package com.fiozxr.yoursql.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fiozxr.yoursql.data.database.dao.*
import com.fiozxr.yoursql.data.database.entity.*

@Database(
    entities = [
        DatabaseInfoEntity::class,
        TableInfoEntity::class,
        ColumnInfoEntity::class,
        ApiKeyEntity::class,
        RlsPolicyEntity::class,
        UserEntity::class,
        BucketEntity::class,
        StorageFileEntity::class,
        QueryHistoryEntity::class,
        SqlFunctionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MasterDatabase : RoomDatabase() {
    abstract fun databaseInfoDao(): DatabaseInfoDao
    abstract fun tableInfoDao(): TableInfoDao
    abstract fun columnInfoDao(): ColumnInfoDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun rlsPolicyDao(): RlsPolicyDao
    abstract fun userDao(): UserDao
    abstract fun bucketDao(): BucketDao
    abstract fun storageFileDao(): StorageFileDao
    abstract fun queryHistoryDao(): QueryHistoryDao
    abstract fun sqlFunctionDao(): SqlFunctionDao

    companion object {
        const val DATABASE_NAME = "yoursql_master.db"
    }
}
