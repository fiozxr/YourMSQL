package com.fiozxr.yoursql.data.database.dao

import androidx.room.*
import com.fiozxr.yoursql.data.database.entity.RlsPolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RlsPolicyDao {

    @Query("SELECT * FROM rls_policies WHERE tableId = :tableId")
    fun getByTable(tableId: Long): Flow<List<RlsPolicyEntity>>

    @Query("SELECT * FROM rls_policies WHERE tableId = :tableId")
    suspend fun getByTableSync(tableId: Long): List<RlsPolicyEntity>

    @Query("SELECT * FROM rls_policies WHERE tableId = :tableId AND scope = :scope LIMIT 1")
    suspend fun getByTableAndScope(tableId: Long, scope: String): RlsPolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(policy: RlsPolicyEntity)

    @Update
    suspend fun update(policy: RlsPolicyEntity)

    @Delete
    suspend fun delete(policy: RlsPolicyEntity)

    @Query("DELETE FROM rls_policies WHERE tableId = :tableId")
    suspend fun deleteByTable(tableId: Long)
}
