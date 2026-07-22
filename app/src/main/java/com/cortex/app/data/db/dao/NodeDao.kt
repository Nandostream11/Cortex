package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cortex.app.data.db.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: NodeEntity)

    @Update
    suspend fun update(node: NodeEntity)

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getById(id: String): NodeEntity?

    /** Used by node canonicalization to find a candidate to merge into. */
    @Query("SELECT * FROM nodes WHERE canonicalName = :canonicalName AND type = :type LIMIT 1")
    suspend fun findByCanonicalName(canonicalName: String, type: String): NodeEntity?

    @Query("SELECT * FROM nodes WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE type = :type")
    suspend fun getAllForType(type: String): List<NodeEntity>

    @Query("SELECT * FROM nodes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes")
    suspend fun getAll(): List<NodeEntity>

    @Query("UPDATE nodes SET importanceScore = :score WHERE id = :id")
    suspend fun updateImportanceScore(id: String, score: Double)

    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun delete(id: String)
}
