package com.cortex.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cortex.app.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status != 'DONE' AND status != 'CANCELLED' ORDER BY priority DESC, dueAt ASC")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String): Flow<List<TaskEntity>>

    /** Unfiltered — used by export/import so done/cancelled tasks aren't silently dropped. */
    @Query("SELECT * FROM tasks")
    suspend fun getAllForExport(): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)
}
