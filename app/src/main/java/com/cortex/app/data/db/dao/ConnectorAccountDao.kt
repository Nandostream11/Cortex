package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cortex.app.data.db.entity.ConnectorAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectorAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: ConnectorAccountEntity)

    @Update
    suspend fun update(account: ConnectorAccountEntity)

    @Query("SELECT * FROM connector_accounts ORDER BY name ASC")
    fun observeAll(): Flow<List<ConnectorAccountEntity>>

    @Query("SELECT * FROM connector_accounts WHERE enabled = 1")
    suspend fun getEnabled(): List<ConnectorAccountEntity>

    @Query("DELETE FROM connector_accounts WHERE id = :id")
    suspend fun delete(id: String)
}
