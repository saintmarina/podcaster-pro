package com.saintmarina.recordingsystem.googleDrive

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
                      private val drive: GoogleDrive,
                      var onStatusChange: (value: String) -> Unit
) {
    private val tag: String = "GoogleDriveFile (${file.name})"
    private val fileSize = file.length()

    private fun readMetadata(file: File): FileMetadata {
        val metadataFile = FileMetadata.pathForFile(file)
        return if (metadataFile.exists()) {
                    try {
                        FileMetadata.deserializeFromJson(metadataFile)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to deserialize metadata file: $e")
                        FileMetadata()
                    }
                } else {
                    FileMetadata()
                }
    }

    fun upload() {
        val metadata = readMetadata(file)
        if (metadata.uploaded) {
            Log.i(tag, "File already uploaded")
            return
        }

        val (startPosition: Long, session:String) =
            if (metadata.sessionUrl == null) {
                val session = createSession()
                metadata.sessionUrl = session
                metadata.serializeToJson(file)
                Log.i(tag, "Creating a new session")
                Pair(0L, session)
            } else {
                val start = getPosFromResumedSession(metadata.sessionUrl!!)
                Log.i(tag, "Resuming the upload session")
                Pair(start, metadata.sessionUrl!!)
            }
        if (startPosition != fileSize) {
            uploadFile(startPosition, session)
        }
        metadata.uploaded = true
        metadata.serializeToJson(file)
        Log.i(TAG, "uploaded")
    }

    private fun uploadFile(startPosition: Long, sessionUri: String) {
        val fileIS = FileInputStream(file)
        fileIS.channel.position(startPosition)
        uploadChunk(sessionUri, fileIS)
        fileIS.close()
    }

    private fun uploadChunk(sessionUri: String, fileIS: FileInputStream): Boolean {
        val url = URL(sessionUri)
        val chunkStart = fileIS.channel.position()
        val request = url.openConnection() as HttpURLConnection
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Length", "$fileSize")
            setRequestProperty("Content-Range", "bytes $chunkStart-${fileSize-1}/${fileSize}")
            setRequestProperty("Accept","*/*")
            copyFromTo(fileIS, outputStream)
            outputStream.close()
        }
        ensureRequestSuccessful(request)
        return request.responseCode == 308
    }

    private fun copyFromTo(fileIS: FileInputStream, fileOS: OutputStream) {
        val byteArray = ByteArray(100 * KB_IN_BYTE)
        var progressCount = 0
        while (true) {
            val bytesRead = fileIS.read(byteArray)
            if (bytesRead == -1) {
                break
            }
            fileOS.write(byteArray, 0, bytesRead)
            progressCount += bytesRead
            Log.d(tag, "progressCount == $progressCount, bytesRead = $bytesRead")
            reportProgress(progressCount)
        }
    }

    private fun reportProgress(bytesUploaded: Int, bytesTotal: Long = fileSize) {
        val percent = "${(bytesUploaded.toDouble()/bytesTotal * 100).toInt()}%"
        val message = "${file.name} $percent uploaded."
        onStatusChange(message)
    }

    private fun createSession(): String {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        val body = "{\"name\": \"${file.name}\", \"parents\": [\"${getDriveIdFromFileParent()}\"]}"

        val request = drive.openRequest(url)
        request.apply {
            requestMethod = "POST"
            doOutput = true //indicates that the application intends to write data to the URL connection.
            setRequestProperty("X-Upload-Content-Type", "audio/wav") //"audio/wav"
            setRequestProperty("X-Upload-Content-Length", "$fileSize}")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Content-Length", "${body.toByteArray().size}")
            outputStream.write(body.toByteArray())
            outputStream.close()
            //connect()
        }

        ensureRequestSuccessful(request)
        Log.i(tag, "Upload session created")
        return request.getHeaderField("location")
    }

    private fun getPosFromResumedSession(sessionUri: String): Long {
        Log.d(tag, "getPosFromResumed()")
        val url = URL(sessionUri)
        val request = drive.openRequest(url)
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Range", "bytes */$fileSize")
        }

        ensureRequestSuccessful(request)
        return when (request.responseCode) {
            308 -> {
                Log.d(tag, "Response: ${request.responseCode}")
                val range = request.getHeaderField("range") ?: return 0
                range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
            }
            200, 201 -> {
                Log.d(tag, "it's 200OK")
                fileSize
            }
            else -> throw ConnectionNotEstablished("Weren't able to connect to Interrupted Upload.Error:${request.responseCode}.${Util.readString(request.errorStream)} ")
        }
    }

    private fun retryUpload() {
        val metadata = FileMetadata()
        metadata.serializeToJson(file)
    }

    private fun ensureRequestSuccessful(request: HttpURLConnection) {
        if (request.responseCode in 400..499) {
            retryUpload()
            throw Exception("${request.responseCode}: ${Util.readString(request.errorStream)}")
        }
        try {
            request.inputStream // Will raise an IOException if not successful.
        } catch (e: IOException) {
            throw IOException("Drive request failed. Error: ${request.responseCode}: $e ${Util.readString(request.errorStream)}", e)
        }
    }

    private fun getDriveIdFromFileParent(): String {
        return DESTINATIONS.find { dest ->
            dest.localDir == file.parent
        }?.driveID ?:
            throw Exception("Lookup of associated drive location failed. Contact the developer")
    }
}