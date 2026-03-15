package com.naigebao.storage.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN lastSyncedAt INTEGER")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN type TEXT NOT NULL DEFAULT 'text'")
            db.execSQL("ALTER TABLE messages ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE messages ADD COLUMN lastAttemptAt INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN expiresAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_sessionId_timestamp ON messages(sessionId, timestamp)")
            db.execSQL("ALTER TABLE sessions ADD COLUMN lastMessagePreview TEXT")
        }
    }

    val all = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
