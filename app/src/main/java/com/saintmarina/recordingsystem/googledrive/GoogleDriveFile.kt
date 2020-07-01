package com.saintmarina.recordingsystem.googledrive

import android.util.Log
import com.saintmarina.recordingsystem.Destination
import com.saintmarina.recordingsystem.Util
import com.saintmarina.recordingsystem.db.FileMetadata
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

const val KB_IN_BYTES = 1000

class FileSyncStatus(val message: String, val error: Boolean, val date: Date? = null)

class GoogleDriveFile(
    private val file: File,
    private val dest: Destination,
    private val drive: GoogleDrive
) {
    private val tag: String = "GoogleDriveFile (${file.name})"
    private val fileSize = file.length()
    var onStatusChange: ((value: FileSyncStatus) -> Unit)? = null

    override fun toString(): String {
        return tag
    }

    private fun reportSuccessStatus(msg: String) {
        onStatusChange?.invoke(FileSyncStatus(message= "\"${file.name}\" $msg for ${dest.cardName}", error=false))
    }

    private fun reportSuccessStatusWithDate(msg: String) {
        onStatusChange?.invoke(FileSyncStatus(message= "\"${file.name}\" $msg for ${dest.cardName}", error=false, date=Date()))
    }

    fun reportErrorStatus(msg: String) {
        onStatusChange?.invoke(FileSyncStatus(message= "\"${file.name}\" $msg for ${dest.cardName}", error=true))
    }

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

        reportSuccessStatusWithDate("uploaded")
    }

    private fun uploadFile(startPosition: Long, sessionUri: String) {
        if (startPosition == fileSize)
            return

        val fs = FileInputStream(file).apply {
            channel.position(startPosition)
        }

        val request = URL(sessionUri).openConnection() as HttpURLConnection
        request.run {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Length", "$fileSize")
            setRequestProperty("Content-Range", "bytes $startPosition-${fileSize - 1}/${fileSize}")
            setRequestProperty("Accept", "*/*")
            setFixedLengthStreamingMode(fileSize - startPosition)
            copyFromTo(fs, outputStream)
            outputStream.close()
            ensureRequestSuccessful(this)
        }

        fs.close()
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

    fun reportProgress(bytesUploaded: Int) {
        val percent = (bytesUploaded.toDouble()/fileSize * 100).toInt()
        if (percent == 100)
            reportSuccessStatus("almost done uploading")
        else
            reportSuccessStatus("$percent% uploaded")
    }

    private fun createSession(): String {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        val body = "{\"name\": \"${file.name}\", \"parents\": [\"${dest.driveID}\"]}"

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
}