package com.cortex.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        /**
         * v1 -> v2: adds NodeEntity.subtype (nullable) and NodeEntity.importanceScore
         * (defaulted), for the graph engine's EntityKind classification and ranking
         * (ADR-0002). Additive only — existing nodes get subtype=NULL,
         * importanceScore=0.0, nothing is dropped or rewritten. This is deliberately a
         * real migration, not fallbackToDestructiveMigration(): CodingStandards.md rules
         * out silently destroying user data, and by Phase 2 that data is memories, not
         * scaffolding.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nodes ADD COLUMN subtype TEXT")
                db.execSQL("ALTER TABLE nodes ADD COLUMN importanceScore REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var instance: CortexDatabase? = null

        /**
         * Room's SQLite file is not encrypted at the framework level; the sensitive
         * surface (API keys, connector tokens) is kept out of it entirely and lives in
         * [com.cortex.app.security.SecretStore] instead. Full at-rest DB encryption
         * (SQLCipher or equivalent) is tracked as a hardening follow-up — see
         * docs/PHASE1_STATUS.md and docs/PHASE2_STATUS.md, not silently assumed here.
         */
        fun getInstance(context: Context): CortexDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CortexDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
