package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cortex.app.data.db.entity.EdgeEntity

@Dao
interface EdgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edge: EdgeEntity)

    @Query("SELECT * FROM edges WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun edgesTouching(nodeId: String): List<EdgeEntity>

    @Query(
        "SELECT * FROM edges WHERE fromNodeId = :fromNodeId AND toNodeId = :toNodeId AND relationType = :relationType LIMIT 1"
    )
    suspend fun findExisting(fromNodeId: String, toNodeId: String, relationType: String): EdgeEntity?

    /** Called when a relation repeats — reinforces the edge instead of duplicating it. */
    @Query("UPDATE edges SET weight = weight + :increment WHERE id = :id")
    suspend fun reinforce(id: String, increment: Double)

    @Query("SELECT * FROM edges ORDER BY weight DESC")
    suspend fun getAllOrderedByWeight(): List<EdgeEntity>

    @Query("DELETE FROM edges WHERE id = :id")
    suspend fun delete(id: String)
}
