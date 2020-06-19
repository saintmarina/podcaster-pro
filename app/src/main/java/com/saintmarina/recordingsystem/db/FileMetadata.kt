package com.saintmarina.recordingsystem.db

import androidx.room.*
import java.io.File

private const val TAG = "FileMetadata"

@Entity
data class FileMetadata(
    @PrimaryKey val path: String,
    @ColumnInfo var uploaded: Boolean = false,
    @ColumnInfo var sessionUrl: String? = null
) {
    fun save() {
        dao().update(this)
    }

    fun delete() {
        dao().delete(this)
    }

    companion object {
        private fun dao(): FileMetadataDao {
            return Database.INSTANCE.fileMetadataDao()
        }

        private fun create(file: File): FileMetadata {
            return FileMetadata(file.path).also {
                dao().insert(it)
            }
        }

        private fun get(file: File): FileMetadata? {
            return dao().get(file.path)
        }

        // returns fileMetadata associated with the 'file' argument
        fun associatedWith(file: File): FileMetadata {
            return get(file) ?: create(file)
        }
    }
}

@Dao
interface FileMetadataDao {
    @Insert
    fun insert(metadata: FileMetadata)

    @Update
    fun update(metadata: FileMetadata)

    @Delete
    fun delete(metadata: FileMetadata)

    @Query("SELECT * FROM FileMetadata WHERE path == :path LIMIT 1")
    fun get(path: String): FileMetadata?
}