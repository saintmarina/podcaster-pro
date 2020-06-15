package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.charset.Charset

private const val JSON_EXT: String = ".metadata.json"
private const val TAG: String = "FileMetadata"

class FileMetadata() {
    var uploaded: Boolean = false
    var sessionUrl: String? = null

    fun serializeToJson(path: File) {
        val file = File(path.path + ".metadata.json" )
        val jsonContent: String = Gson().toJson(this)
        val outputStream = FileOutputStream(file)
        outputStream.write(jsonContent.toByteArray())
        outputStream.flush()
        outputStream.close()
    }

    companion object {
        fun deserializeFromJson(jsonFile: File): FileMetadata {
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

        fun pathForFile(file: File): File {
            return File(file.path + JSON_EXT)
        }
    }
}
