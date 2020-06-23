package com.saintmarina.recordingsystem.db

import android.content.Context
import androidx.room.*

@androidx.room.Database(entities = [FileMetadata::class], version = 1)
abstract class Database: RoomDatabase() {
    abstract fun fileMetadataDao(): FileMetadataDao

    companion object {
        lateinit var INSTANCE: Database

        fun init(context: Context) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                Database::class.java, "database.db"
            ).build()
        }
    }
}
