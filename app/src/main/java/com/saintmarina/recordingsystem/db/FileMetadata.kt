package com.saintmarina.recordingsystem.db

import android.util.Log
import java.io.File

private const val TAG = "FileMetadata"

class FileMetadata() {
    private val dao = db!!.metadataDao()
    var uploaded: Boolean = false
    var sessionUrl: String? = null
    private var associatedFile: File? = null

    private fun getMetadataEntity(): FileMetadataEntity {
        return FileMetadataEntity(associatedFile.toString(), uploaded, sessionUrl)
    }

    fun save() {
        dao.updateMetadataFile(getMetadataEntity())
    }

    fun delete() {
        dao.deleteMetadataFile(getMetadataEntity())
    }

    companion object {
        private val db = MetadataDatabase.INSTANCE

        private fun fromEntityToFileMetadataObj(metadataEntity: FileMetadataEntity): FileMetadata {
            return FileMetadata().apply {
                associatedFile = File(metadataEntity.fileName)
                uploaded = metadataEntity.uploaded
                sessionUrl = metadataEntity.sessionUri
            }
        }

        private fun retrieveFromDb(file: File): FileMetadataEntity? {
            return db!!.metadataDao().getMetadataFile(file.toString())
        }

        fun associatedWith(file: File): FileMetadata {
            return when (val metadataEntity = retrieveFromDb(file)) {
                null -> {
                    FileMetadata().apply { associatedFile = file }
                        .apply { dao.insertMetadataFile(FileMetadataEntity(associatedFile.toString(), uploaded, sessionUrl)) }
                }
                else -> fromEntityToFileMetadataObj(metadataEntity)
            }
        }
    }
}
