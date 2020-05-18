package com.saintmarina.recordingsystem.GoogleDrive

//import java.io.File
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


//https://drive.google.com/drive/folders/1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l?usp=sharing

private const val TAG = "Google Drive"
private const val MAX_CHUNK_SIZE: Long = 262144
class UploadUnsuccessfulException(message: String): Exception(message)
class ConnectionNotEstablished(message: String): Exception(message)


class GoogleDrive(credFile: InputStream) {
    private val credential = GoogleCredential.fromStream(credFile).createScoped(DriveScopes.all())
    private val httpTransport = NetHttpTransport() //GoogleNetHttpTransport.newTrustedTransport()
    private var service = Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("Recording System")
            .build()

    fun uploadToDrive(job: FileUploadJob) {//This should check for info from FileUploadJob and figure out if to start new upload or continue an existing one
        val file = job.file
        val metadata = job.metadata

        if (metadata.sessionUrl != null) {
            resumeExistingUpload(metadata.sessionUrl!!, file) // if unsuccessful trowing exceptions
            // If upload succeed change the metadata of the file
        } else {
            startNewUpload(file) // if unsuccessful trowing exceptions
        }
    }

    private fun resumeExistingUpload(sessionUri: String, file: java.io.File) {
         val request = connectToInterruptedUpload(sessionUri, file.length())
         val range: String = request.getHeaderField("range")
         Log.e(TAG, range)
         val chunkStart = range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
         uploadFile(chunkStart, sessionUri, file)
    }

    private fun connectToInterruptedUpload(sessionUri: String, fileSize: Long): HttpURLConnection {
        val url = URL(sessionUri)
        val request = url.openConnection() as HttpURLConnection
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Length", "0")
            setRequestProperty("Content-Range", "bytes */$fileSize")
            connect()
        }

        when (request.responseCode) {
            308 -> return request
            else -> throw ConnectionNotEstablished("Weren't able to connect to Interrupted Upload")
        }
    }

    private fun uploadFile(pos: Long, sessionUri: String, file: java.io.File) {
        val fileSize = file.length()
        val chunkSize = if (fileSize > MAX_CHUNK_SIZE) MAX_CHUNK_SIZE else fileSize
        var chunkPosition: Long = pos
        for (byte in 0 until fileSize step chunkSize) {
            Log.e(TAG, "chunkPosition == $chunkPosition")
            chunkPosition = uploadChunk(chunkSize, chunkPosition, sessionUri, fileSize, file)
            if (chunkPosition == -1L) Log.e(TAG, "Upload successfully finished")
        }
        Log.e(TAG, "AFTER UPLOADING A FILE")
    }

    private fun startNewUpload(file: java.io.File) {
        val sessionUri = initUploadSession(file.length(), file)
        uploadFile(0, sessionUri, file)
    }

    private fun uploadChunk(chunkSize: Long, chunkStart: Long, sessionUri: String, fileSize: Long, file: java.io.File): Long {
        val url = URL(sessionUri)
        var uploadedBytes: Long = chunkSize

        if (chunkStart + uploadedBytes > fileSize) {
            uploadedBytes = fileSize - chunkStart
        }

        val buffer = ByteArray(uploadedBytes.toInt())
        val fileInputStream = FileInputStream(file)
        fileInputStream.channel.position(chunkStart)
        val bytesRead = fileInputStream.read(buffer, 0, uploadedBytes.toInt())
        if (bytesRead == -1) Log.e(TAG, "no bytes loaded")
        fileInputStream.close()

        val request = url.openConnection() as HttpURLConnection
        request.apply {
            doOutput = true
            requestMethod = "PUT"
            connectTimeout = 10000
            setRequestProperty("Content-Length", "$uploadedBytes")
            setRequestProperty("Content-Range", "bytes $chunkStart-${chunkStart + uploadedBytes-1}/${fileSize}")
            setRequestProperty("Accept","*/*")
            outputStream.write(buffer)
            outputStream.close()
           // connect()
        }

        return when (request.responseCode) {
            308 -> {
                val range: String = request.getHeaderField("range")
                Log.e(TAG, range)
                range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
            }
            200 -> -1 //Success
            else -> {
                throw UploadUnsuccessfulException("Upload of chunk $chunkStart - ${chunkStart + uploadedBytes-1} didn't succeed." +
                        "Response Code: ${request.responseCode}" +
                        "Response message: ${request.responseMessage}" +
                        "Error: ${readString(request.errorStream)}")
            }
        }
    }

    private fun initUploadSession(fileSize: Long, file: java.io.File): String {
        Log.e(TAG, "inside initUploadSession")
        credential.expirationTimeMilliseconds
        credential.refreshToken()

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        val body = "{\"name\": \"${file.name}\", \"parents\": [\"1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l\"]}"

        val request = url.openConnection() as HttpURLConnection
        Log.e(TAG, "access token: ${credential.accessToken}")
        request.apply {
            requestMethod = "POST"
            doOutput = true //indicates that the application intends to write data to the URL connection.
            setRequestProperty("Authorization", "Bearer " + credential.accessToken)
            setRequestProperty("X-Upload-Content-Type", "audio/wav") //"audio/wav"
            setRequestProperty("X-Upload-Content-Length", "$fileSize")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Content-Length", "${body.toByteArray().size}")
            outputStream.write(body.toByteArray())
            outputStream.close()
            connect()
        }

        return when (request.responseCode) {
            200 -> request.getHeaderField("location")
            else -> throw ConnectionNotEstablished("Weren't able to set a connection")
        }
    }

    private fun readString(inputStream: InputStream): String {
        return String(inputStream.readBytes()) // Or whatever encoding
    }
}
/*
    private fun createFile() {
        val createdFile = FileOutputStream(java.io.File("/sdcard/", "hello.txt")).apply { write("Hello World!".toByteArray())}
        val fileMetadata = File().apply {
            name = "hello.txt"
            parents = Collections.singletonList("1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l")
        }
    }



//should be done by Google Drive after the upload is successfull
    private fun createJsonFile(file: File) {
        val jsonContent = JSONObject().apply {
            put("uploaded", "false")
            put("sessionUrl", "null")
        }
        FileOutputStream(file).apply {write(jsonContent.toString().toByteArray())}
    }
*/




/* LIST FILES IN GOOGLE DRIVE
    val result = service.files().list()
        .setPageSize(10)
        .setFields("nextPageToken, files(id, name)")
        .execute()

    val files: MutableList<com.google.api.services.drive.model.File>? = result.files

    if (files == null || files.isEmpty()) {
        Log.e(TAG, "No files found.")
    } else {
        Log.e(TAG, "Files:")
        for (file in files) {
            Log.e(TAG, "${file.name}, ${file.id}")
        }
    }
 */

/* SIMPLE UPLOAD
    private fun uploadFile(driveService: Drive) {
        //create a file
        val folderId = "1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l"
        val createdFile = FileOutputStream(java.io.File("/sdcard/", "hello.txt"))
        var bytesArray = "Hello World!".toByteArray()
        createdFile.write(bytesArray)
        val fileMetadata = File().apply {
            name = "hello.txt"
            parents = Collections.singletonList(folderId)
        }
        val filePath = java.io.File("/sdcard/", "hello.txt")
        val mediaContent = FileContent("text/xml", filePath)
        val file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
        Log.e(TAG, "File ID: ${file.id}")
    }*/