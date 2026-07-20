package com.cortex.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.cortex.app.data.db.dao.ConnectorAccountDao
import com.cortex.app.data.db.dao.EdgeDao
import com.cortex.app.data.db.dao.GuidanceEventDao
import com.cortex.app.data.db.dao.MemoryDao
import com.cortex.app.data.db.dao.NodeDao
import com.cortex.app.data.db.dao.ProjectDao
import com.cortex.app.data.db.dao.TaskDao
import com.cortex.app.data.db.entity.ConnectorAccountEntity
import com.cortex.app.data.db.entity.EdgeEntity
import com.cortex.app.data.db.entity.GuidanceEventEntity
import com.cortex.app.data.db.entity.MemoryItemEntity
import com.cortex.app.data.db.entity.NodeEntity
import com.cortex.app.data.db.entity.ProjectEntity
import com.cortex.app.data.db.entity.TaskEntity

@Database(
    entities = [
        MemoryItemEntity::class,
        NodeEntity::class,
        EdgeEntity::class,
        TaskEntity::class,
        ProjectEntity::class,
        ConnectorAccountEntity::class,
        GuidanceEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CortexDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun connectorAccountDao(): ConnectorAccountDao
    abstract fun guidanceEventDao(): GuidanceEventDao

    companion object {
        private const val DATABASE_NAME = "cortex.db"

        @Volatile
        private var instance: CortexDatabase? = null

        /**
         * Room's SQLite file is not encrypted at the framework level; the sensitive
         * surface (API keys, connector tokens) is kept out of it entirely and lives in
         * [com.cortex.app.security.SecretStore] instead. Full at-rest DB encryption
         * (SQLCipher or equivalent) is tracked as a hardening follow-up — see the
         * Phase-1 notes in the audit response, not silently assumed here.
         */
        fun getInstance(context: Context): CortexDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CortexDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
