package com.exodidio.mimi.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Track::class, MetaEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class MimiDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    abstract fun metaDao(): MetaDao

    companion object {
        private const val NAME = "mimi.db"

        @Volatile
        private var instance: MimiDatabase? = null

        fun get(context: Context): MimiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, MimiDatabase::class.java, NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
