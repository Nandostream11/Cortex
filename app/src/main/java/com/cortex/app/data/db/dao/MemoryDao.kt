package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cortex.app.data.db.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MemoryItemEntity)

    @Update
    suspend fun update(item: MemoryItemEntity)

    @Query("SELECT * FROM memory_items WHERE id = :id")
    suspend fun getById(id: String): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE isPinned = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<MemoryItemEntity>>

    @Query(
        """
        SELECT * FROM memory_items
        WHERE isArchived = 0 AND (rawText LIKE '%' || :query || '%' OR normalizedText LIKE '%' || :query || '%')
        ORDER BY importanceScore DESC, createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchByKeyword(query: String, limit: Int = 50): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE isArchived = 0 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<MemoryItemEntity>

    /** Unfiltered — used by export/import so archived items aren't silently dropped. */
    @Query("SELECT * FROM memory_items")
    suspend fun getAllForExport(): List<MemoryItemEntity>

    @Query("UPDATE memory_items SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)

    @Query("UPDATE memory_items SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM memory_items WHERE isArchived = 0")
    fun observeActiveCount(): Flow<Int>
}
