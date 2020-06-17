package com.saintmarina.recordingsystem.googleDrive

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.channels.FileChannel
import java.nio.charset.Charset

private const val JSON_EXT: String = ".metadata.json"
private const val TAG = "FileMetadata"

class FileMetadata() {
    var uploaded: Boolean = false
    var sessionUrl: String? = null

    private var associatedFile: File? = null

    fun save() {
        val file = associatedMetadataPath(associatedFile!!)
        val jsonContent: String = Gson().toJson(this)
        val outputStream = FileOutputStream(file)
        outputStream.write(jsonContent.toByteArray())
        outputStream.flush()
        outputStream.close()
    }

    fun delete() {
        associatedMetadataPath(associatedFile!!).delete()
    }

    companion object {
        private fun deserializeFromJson(jsonFile: File): FileMetadata {
            val fileContent = readFileContent(jsonFile)
            val jsonObj = JSONObject(fileContent)

            return FileMetadata().apply {
                uploaded = jsonObj.getBoolean("uploaded")
                sessionUrl = jsonObj.getString("sessionUrl")
            }
        }

        private fun readFileContent(file: File): String {
            val stream = FileInputStream(file)
            val byteBuffer =
                stream.channel.map(FileChannel.MapMode.READ_ONLY, 0, stream.channel.size())
            return Charset.defaultCharset().decode(byteBuffer).toString()
        }

        private fun associatedMetadataPath(file: File): File {
            return File(file.path + JSON_EXT)
        }

        fun associatedWith(file: File): FileMetadata {
            val metadataFile = associatedMetadataPath(file)
            val metadata = if (metadataFile.exists()) {
                try {
                    deserializeFromJson(metadataFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to deserialize metadata file: $e")
                    FileMetadata()
                }
            } else {
                FileMetadata()
            }
            metadata.associatedFile = file
            return metadata
        }
    }
}
