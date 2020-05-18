package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.charset.Charset

private const val TAG: String = "FileMetadata"



class FileMetadata() {
    var uploaded: Boolean = false
    var sessionUrl: String? = null

    companion object {
        fun fromJsonFile(jsonFile: File): FileMetadata {
            val fileContent = readFileContent(jsonFile)
            val jsonObj = JSONObject(fileContent)
            Log.e(TAG, "jsonStr = ${fileContent}")

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
    }
}

    /*  //TODO: Ask whether we need this when, using JSONObject
     fun  default(): FileMetadata
      {
          return FileMetadata( false, null )
      }

      fun metadataToJson(): String
      {
          return json.stringy(this)
      }*/
