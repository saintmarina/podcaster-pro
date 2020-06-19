package com.saintmarina.recordingsystem.db

import android.content.Context
import androidx.room.*
import java.io.File
import javax.annotation.Nullable


//TODO replicate the FileMetadata object
// TODO Replace this with a https://developer.android.com/training/data-storage/room entity+Dao
// There should be a new folder in recordingsystem: db (like service or ui)
// In this folder, there should be a Database.kt and a FileMetadata.kt file // TODO Do this first
// all folders should be lowercase, so UI -> ui, googleDrive -> googledrive

@Database(entities = [FileMetadataEntity::class], version = 1)
abstract class MetadataDatabase: RoomDatabase() {
    abstract fun metadataDao(): MetadataDao

    companion object {
        var INSTANCE: MetadataDatabase? = null

        fun init(context: Context) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                MetadataDatabase::class.java, "metadata.db"
            ).build()
        }
    }
}

@Entity // TODO put save() and delete() in the entity
data class FileMetadataEntity(
    @PrimaryKey val fileName: String,
    @ColumnInfo var uploaded: Boolean,
    @ColumnInfo var sessionUri: String?
)

@Dao
interface MetadataDao {
    @Insert
    fun insertMetadataFile(metadata: FileMetadataEntity)

    @Update
    fun updateMetadataFile(metadata: FileMetadataEntity)

    @Delete
    fun deleteMetadataFile(metadata: FileMetadataEntity)

    @Query("SELECT * FROM FileMetadataEntity WHERE fileName == :name LIMIT 1")
    fun getMetadataFile(name: String): FileMetadataEntity?


}