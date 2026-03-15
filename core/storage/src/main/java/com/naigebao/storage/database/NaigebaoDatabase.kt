package com.naigebao.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.naigebao.storage.database.dao.MessageDao
import com.naigebao.storage.database.dao.SessionDao
import com.naigebao.storage.database.dao.UserDao
import com.naigebao.storage.database.entity.MessageEntity
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.database.entity.UserEntity
import com.naigebao.storage.database.migration.DatabaseMigrations
import com.naigebao.storage.encryption.DatabaseEncryption
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NaigebaoDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun messageDao(): MessageDao

    abstract fun userDao(): UserDao

    companion object {
        fun create(context: Context): NaigebaoDatabase {
            val passphrase = DatabaseEncryption.passphrase(context)
            return Room.databaseBuilder(context, NaigebaoDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(SupportFactory(passphrase))
                .addMigrations(*DatabaseMigrations.all)
                .build()
        }

        private const val DATABASE_NAME = "naigebao.db"
    }
}
