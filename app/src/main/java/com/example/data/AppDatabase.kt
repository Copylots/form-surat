package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LetterDraft::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun letterDraftDao(): LetterDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "letter_generator_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
