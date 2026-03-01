package com.fiozxr.yoursql.di

import android.content.Context
import androidx.room.Room
import com.fiozxr.yoursql.data.database.MasterDatabase
import com.fiozxr.yoursql.data.repository.*
import com.fiozxr.yoursql.domain.repository.DatabaseRepository
import com.fiozxr.yoursql.domain.repository.QueryExecutor
import com.fiozxr.yoursql.domain.repository.TableRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDatabaseRepository(
        impl: DatabaseRepositoryImpl
    ): DatabaseRepository

    @Binds
    @Singleton
    abstract fun bindTableRepository(
        impl: TableRepositoryImpl
    ): TableRepository

    @Binds
    @Singleton
    abstract fun bindQueryExecutor(
        impl: QueryExecutorImpl
    ): QueryExecutor

    companion object {
        @Provides
        @Singleton
        fun provideMasterDatabase(@ApplicationContext context: Context): MasterDatabase {
            return Room.databaseBuilder(
                context,
                MasterDatabase::class.java,
                MasterDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideDatabaseRepositoryImpl(
            @ApplicationContext context: Context,
            masterDb: MasterDatabase
        ): DatabaseRepositoryImpl {
            return DatabaseRepositoryImpl(context, masterDb)
        }

        @Provides
        @Singleton
        fun provideTableRepositoryImpl(
            masterDb: MasterDatabase,
            databaseRepository: DatabaseRepositoryImpl
        ): TableRepositoryImpl {
            return TableRepositoryImpl(masterDb, databaseRepository)
        }

        @Provides
        @Singleton
        fun provideQueryExecutorImpl(
            masterDb: MasterDatabase,
            databaseRepository: DatabaseRepositoryImpl
        ): QueryExecutorImpl {
            return QueryExecutorImpl(masterDb, databaseRepository)
        }

        @Provides
        @Singleton
        fun provideApiKeyRepositoryImpl(
            masterDb: MasterDatabase
        ): ApiKeyRepositoryImpl {
            return ApiKeyRepositoryImpl(masterDb)
        }

        @Provides
        @Singleton
        fun provideUserRepositoryImpl(
            masterDb: MasterDatabase
        ): UserRepositoryImpl {
            return UserRepositoryImpl(masterDb)
        }

        @Provides
        @Singleton
        fun provideStorageRepositoryImpl(
            @ApplicationContext context: Context,
            masterDb: MasterDatabase
        ): StorageRepositoryImpl {
            return StorageRepositoryImpl(context, masterDb)
        }
    }
}
