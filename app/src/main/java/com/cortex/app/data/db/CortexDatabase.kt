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
import com.cortex.app.security.DatabasePassphraseProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
         * scaffolding. SQLCipher is transparent at the SQL level, so this migration's
         * ALTER TABLE statements are unaffected by encryption being added below.
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
         * Opens the database through SQLCipher (`net.zetetic:sqlcipher-android`,
         * `SupportOpenHelperFactory`) rather than plain Room, satisfying Security.md's
         * "encrypt sensitive local files" — the graph now makes *inferred relationships
         * between memories* queryable in the DB file, not just raw captured text, which
         * made this the top-priority gap coming out of Phase 2 (see PHASE2_STATUS.md).
         *
         * The passphrase itself is generated once and stored Keystore-backed via
         * [DatabasePassphraseProvider] — see that class's doc comment for why this is a
         * stored-random-key design rather than a Keystore-native key used directly
         * (SQLCipher's API needs a raw passphrase byte array, Keystore doesn't hand
         * those out).
         *
         * `System.loadLibrary("sqlcipher")` is required once per process before any
         * SQLCipher database operation, per the library's integration docs.
         *
         * VERIFICATION NOTE: this integration could not be exercised in the sandbox this
         * was written in — SQLCipher requires native (JNI) binaries that only run on a
         * real Android runtime, not a plain JVM, so the "compile and run against a real
         * Kotlin compiler" method used elsewhere in this codebase (see
         * docs/PHASE2_STATUS.md) does not apply here. This was written carefully against
         * current official documentation (net.zetetic:sqlcipher-android's own README,
         * fetched via web search this session) rather than from training-data memory,
         * since the library was renamed and its integration API changed after this
         * assistant's training cutoff — but it is unverified beyond that. Treat getting
         * this specific integration working as its own explicit checkpoint when this
         * project first reaches a real Android build, not a detail to assume works
         * alongside everything else.
         */
        fun getInstance(context: Context, passphraseProvider: DatabasePassphraseProvider): CortexDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    System.loadLibrary("sqlcipher")
                    val factory = SupportOpenHelperFactory(passphraseProvider.getOrCreatePassphrase())
                    Room.databaseBuilder(
                        context.applicationContext,
                        CortexDatabase::class.java,
                        DATABASE_NAME
                    )
                        .openHelperFactory(factory)
                        .addMigrations(MIGRATION_1_2)
                        .build()
                }.also { instance = it }
            }
    }
}
