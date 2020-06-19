package com.saintmarina.recordingsystem.db

import android.content.Context
import androidx.room.*


//T ODO replicate the FileMetadata object
// TODO Replace this with a https://developer.android.com/training/data-storage/room entity+Dao
// There should be a new folder in recordingsystem: db (like service or ui)
// In this folder, there should be a Database.kt and a FileMetadata.kt file // TODO Do this first
// all folders should be lowercase, so UI -> ui, googleDrive -> googledrive

@androidx.room.Database(entities = [FileMetadataEntity::class], version = 1)
abstract class Database: RoomDatabase() {
    abstract fun metadataDao(): MetadataDao

    companion object {
        lateinit var INSTANCE: Database

        fun init(context: Context) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                Database::class.java, "metadata.db"
            ).build()
        }
    }
}
