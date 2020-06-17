package com.saintmarina.recordingsystem.googleDrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Util
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

const val KB_IN_BYTES = 1000

class GoogleDriveFile(val file: File,
                      private val drive: GoogleDrive,
                      var onStatusChange: (FileStatus) -> Unit
) {
    private val tag: String = "GoogleDriveFile (${file.name})"
    private val fileSize = file.length()

    fun upload() {
        val metadata = FileMetadata.associatedWith(file)
        if (metadata.uploaded) {
            Log.i(tag, "File already uploaded")
            return
        }

        val (startPosition: Long, session:String) =
            if (metadata.sessionUrl == null) {
                val session = createSession()
                metadata.sessionUrl = session
                metadata.save()
                Log.i(tag, "Creating a new session")
                Pair(0L, session)
            } else {
                val start = getPosFromResumedSession(metadata.sessionUrl!!)
                Log.i(tag, "Resuming the upload session")
                Pair(start, metadata.sessionUrl!!)
            }

        uploadFile(startPosition, session)

        metadata.uploaded = true
        metadata.save()
        Log.i(tag, "uploaded")
    }

    private fun uploadFile(startPosition: Long, sessionUri: String) {
        if (startPosition == fileSize)
            return

        val fileIS = FileInputStream(file)
        val chunkStart = fileIS.channel.position(startPosition)
        val url = URL(sessionUri)
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
        fileIS.close()
    }

    private fun copyFromTo(fileIS: FileInputStream, fileOS: OutputStream) {
        val byteArray = ByteArray(100 * KB_IN_BYTES)
        var progressCount = 0
        while (true) {
            val bytesRead = fileIS.read(byteArray)
            if (bytesRead == -1) {
                break
            }
            fileOS.write(byteArray, 0, bytesRead)
            fileOS.flush()
            progressCount += bytesRead
            reportProgress(progressCount)
        }
    }

    private fun reportProgress(bytesUploaded: Int) {
        val percent = (bytesUploaded.toDouble()/fileSize * 100).toInt()
        val message = "${file.name} $percent% uploaded"
        onStatusChange(FileStatus.success(message))
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
        }

        ensureRequestSuccessful(request)
        Log.i(tag, "Upload session created")
        return request.getHeaderField("location")
    }

    private fun getPosFromResumedSession(sessionUri: String): Long {
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
                val range = request.getHeaderField("range") ?: return 0
                range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
            }
            200, 201 -> fileSize
            else -> throw ConnectionNotEstablished("Weren't able to connect to Interrupted Upload.${request.responseCode}:${Util.readString(request.errorStream)} ")
        }
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

    private fun retryUpload() {
        FileMetadata.associatedWith(file).delete()
    }

    private fun getDriveIdFromFileParent(): String {
        return DESTINATIONS.find { dest ->
            dest.localDir.path == file.parent // Check the values for existence of '/'
        }?.driveID ?:
            throw Exception("Lookup of associated drive location failed. Contact the developer")
    }
}