package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, CuePointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DjDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun cuePointDao(): CuePointDao

    companion object {
        @Volatile
        private var INSTANCE: DjDatabase? = null

        fun getDatabase(context: Context): DjDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DjDatabase::class.java,
                    "traktor_dj.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
