package com.saintmarina.recordingsystem.db

import android.content.Context
import androidx.room.*


// TODO replicate the FileMetadata object
// TODO Replace this with a https://developer.android.com/training/data-storage/room entity+Dao
// There should be a new folder in recordingsystem: db (like service or ui)
// In this folder, there should be a Database.kt and a FileMetadata.kt file // TODO Do this first
// all folders should be lowercase, so UI -> ui, googleDrive -> googledrive

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
