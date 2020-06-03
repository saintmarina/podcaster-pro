package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Util
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GoogleDriveFile"
const val KB_IN_BYTE = 1000

class GoogleDriveFile(val file: File,
                      val metadata: FileMetadata,
                      private val drive: GoogleDrive,
                      var onStatusChange: (value: String) -> Unit
) {
    private val tag: String = "GoogleDriveFile (${file.name})"
    
    fun upload() {
        if (metadata.uploaded) {
            Log.e(tag, "File already uploaded")
            return
        }

        Log.d(tag, "starting upload")
        val (startPosition: Long, session:String) =
            if (metadata.sessionUrl == null) {
                Log.i(tag, "No previous sessionUri. Creating new sessionUri")
                val session = createSession()
                metadata.sessionUrl = session
                Log.i(TAG, "dest localDir ${file.parent}")
                Log.i(TAG, "DRIVE file ${file.path}")
                metadata.serializeToJson(file)
                Pair(0L, session)
            } else {
                Log.i(tag, "Found previous sessionUri. Resuming the session")
                val start = getPosFromResumedSession(metadata.sessionUrl!!)
                Pair(start, metadata.sessionUrl!!)
            }

        uploadFile(startPosition, session)
    }

    private fun uploadFile(startPosition: Long, sessionUri: String) {
        val fileIS = FileInputStream(file)
        fileIS.channel.position(startPosition)
        if (uploadChunk(sessionUri, fileIS)) {
            Log.i(tag, "Upload successfully finished")
        }
        fileIS.close()
    }

    // returns true if there's more chunks to upload, false otherwise
    private fun uploadChunk(sessionUri: String, fileIS: FileInputStream): Boolean {
        val url = URL(sessionUri)
        val fileSize = file.length()

        val chunkStart = fileIS.channel.position()
        val request = url.openConnection() as HttpURLConnection
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Length", "*/*")
            setRequestProperty("Content-Range", "bytes $chunkStart-${fileSize-1}/${fileSize}")
            Log.d(tag, "Content-Range bytes $chunkStart-${fileSize-1}/${fileSize}")
            setRequestProperty("Accept","*/*")
            copyFromTo(fileIS, outputStream)
            outputStream.close()
            // connect()
        }
        ensureRequestSuccessful(request)
        return request.responseCode == 308
    }

    private fun copyFromTo(fileIS: FileInputStream, fileOS: OutputStream) {
        val byteArray = ByteArray(100 * KB_IN_BYTE)

        while (true) {
            val bytesRead = fileIS.read(byteArray)
            if (bytesRead == -1) {
                break
            }
            fileOS.write(byteArray, 0, bytesRead)
            reportProgress(bytesRead)
            Log.d(tag, "bytes uploaded $bytesRead/${file.length()}")
        }
    }

    private fun reportProgress(bytesUploaded: Int, bytesTotal: Long = file.length()) {
        val value = if (bytesUploaded == 0) ""
                    else "$bytesUploaded/$bytesTotal uploaded."
        Log.i(TAG, "reportProgress happened. Value = $value")
        onStatusChange(value)
    }

    private fun createSession(): String {
        Log.e(tag, "inside createSession")

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        val body = "{\"name\": \"${file.name}\", \"parents\": [\"${getDriveIdFromFileParent()}\"]}"

        val request = drive.openRequest(url)

        request.apply {
            requestMethod = "POST"
            doOutput = true //indicates that the application intends to write data to the URL connection.
            setRequestProperty("X-Upload-Content-Type", "audio/wav") //"audio/wav"
            setRequestProperty("X-Upload-Content-Length", "${file.length()}")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Content-Length", "${body.toByteArray().size}")
            outputStream.write(body.toByteArray())
            outputStream.close()
            connect()
        }

        ensureRequestSuccessful(request)
        return request.getHeaderField("location")
    }

    private fun getPosFromResumedSession(sessionUri: String): Long {
        val url = URL(sessionUri)
        val request = drive.openRequest(url)
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            //instanceFollowRedirects = false // We get 308 for resume, which also means to redirect. We don't want the java library to following redirections 20 times
            //setRequestProperty("Content-Length", "0")
            setRequestProperty("Content-Range", "bytes */${file.length()}")
            Log.d(tag, "file length is ${file.length()}")
            connect()
        }

        ensureRequestSuccessful(request)
        when (request.responseCode) {
            308 -> {
                val range = request.getHeaderField("range")
                Log.e(tag, "responseCode = ${request.responseCode}")
                Log.e(tag, "responseMessage = ${request.responseMessage}")
                Log.e(tag, "range is $range")
                return range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
            }
            else -> throw ConnectionNotEstablished("Weren't able to connect to Interrupted Upload")
        }
    }

    private fun ensureRequestSuccessful(request: HttpURLConnection) {
        try {
            request.inputStream // Will raise an IOException if not successful.
        } catch (e: IOException) {
            throw IOException("Something happened: $e ${Util.readString(request.errorStream)}", e)
        }
    }

    private fun getDriveIdFromFileParent(): String {
        // TODO use find() DESTINATIONS.find {  }
        return DESTINATIONS.find { dest ->
            dest.localDir == file.parent
        }?.driveID ?:
            throw Exception("File driveID not found")

    }
}