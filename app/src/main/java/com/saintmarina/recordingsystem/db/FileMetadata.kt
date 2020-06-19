package com.saintmarina.recordingsystem.db

import android.util.Log
import androidx.room.*
import java.io.File

private const val TAG = "FileMetadata"

class FileMetadata() {
    var uploaded: Boolean = false
    var sessionUrl: String? = null
    private var associatedFile: File? = null

    private fun getMetadataEntity(): FileMetadataEntity {
        return FileMetadataEntity(associatedFile.toString(), uploaded, sessionUrl)
    }

    fun save() {
        dao().updateMetadataFile(getMetadataEntity())
    }

    fun delete() {
        dao().deleteMetadataFile(getMetadataEntity())
    }

    companion object {
        fun dao(): MetadataDao {
            return Database.INSTANCE.metadataDao()
        }

        private fun fromEntityToFileMetadataObj(metadataEntity: FileMetadataEntity): FileMetadata {
            return FileMetadata().apply {
                associatedFile = File(metadataEntity.fileName)
                uploaded = metadataEntity.uploaded
                sessionUrl = metadataEntity.sessionUri
            }
        }

        private fun retrieveFromDb(file: File): FileMetadataEntity? {
            return dao().getMetadataFile(file.toString())
        }

        fun associatedWith(file: File): FileMetadata {
            return when (val metadataEntity = retrieveFromDb(file)) {
                null -> {
                    FileMetadata().apply { associatedFile = file }
                        .apply { dao().insertMetadataFile(FileMetadataEntity(associatedFile.toString(), uploaded, sessionUrl)) }
                }
                else -> fromEntityToFileMetadataObj(metadataEntity)
            }
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