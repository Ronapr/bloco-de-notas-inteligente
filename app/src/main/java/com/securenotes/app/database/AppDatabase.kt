package com.securenotes.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.securenotes.app.database.dao.FolderDao
import com.securenotes.app.database.dao.NoteDao
import com.securenotes.app.database.dao.ReminderDao
import com.securenotes.app.database.entity.FolderEntity
import com.securenotes.app.database.entity.NoteEntity
import com.securenotes.app.database.entity.ReminderEntity

@Database(
    entities = [NoteEntity::class, FolderEntity::class, ReminderEntity::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun folderDao(): FolderDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_notes.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
