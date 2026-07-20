package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cortex.app.data.db.entity.GuidanceEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuidanceEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: GuidanceEventEntity)

    @Query("SELECT * FROM guidance_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<GuidanceEventEntity>>

    @Query("SELECT * FROM guidance_events ORDER BY createdAt DESC")
    suspend fun getAllForExport(): List<GuidanceEventEntity>
}
