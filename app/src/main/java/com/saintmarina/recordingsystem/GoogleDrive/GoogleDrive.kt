package com.saintmarina.recordingsystem.GoogleDrive

//import java.io.File
import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


//https://drive.google.com/drive/folders/1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l?usp=sharing

private const val TAG = "Google Drive"
//private const val chunkSizeInMb =

class GoogleDrive(context: Context) {
    private lateinit var sessionUri: String
    val context = context
    private val jsonFactory = JacksonFactory.getDefaultInstance()
    private val appName = "Recording System"
    private var inputStream = context.assets.open("credentials.json")

    private val file = java.io.File("/sdcard/", "hello.txt")
    private var chunkPosition: Long = 0


    init {
        Log.e(TAG, "inside init GoogleDrive")
        val httpTransport = NetHttpTransport() //GoogleNetHttpTransport.newTrustedTransport()

        Thread(Runnable {
            try {
                var service =
                    Drive.Builder(httpTransport, jsonFactory, getCredentials())
                        .setApplicationName(appName)
                        .build()
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

                Log.e(TAG, "BEFORE UPLOADING A FILE")
                startResumableSession()



                for (byte in 0..file.length() - 1 step 2) {
                    Log.e(TAG, "chunkPosition == $chunkPosition")
                    resumeUploadFile(chunkPosition)
                }
                Log.e(TAG, "AFTER UPLOADING A FILE")

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }).start()
    }

    private fun startResumableSession() {
        val creds = getCredentials()
        creds.refreshToken()


        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        val body = "{\"name\": \"${file.name}\", \"parents\": [\"1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l\"]}"

        val request = url.openConnection() as HttpURLConnection
        Log.e(TAG, "access token: ${creds.accessToken}")
        request.apply {
            requestMethod = "POST"
            //doInput = true  //indicates that the application intends to read data from the URL connection.
            doOutput = true //indicates that the application intends to write data to the URL connection.
            setRequestProperty("Authorization", "Bearer " + creds.accessToken)
            setRequestProperty("X-Upload-Content-Type", "text/xml") //"audio/wav"
            setRequestProperty("X-Upload-Content-Length", "${file.length()}")
            Log.e(TAG, "X-Upload-Content-Length== ${file.length()}")

            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Content-Length", "${body.toByteArray().size}")
            outputStream.write(body.toByteArray())
            outputStream.close()
            connect()
        }

        if (request.responseCode == HttpURLConnection.HTTP_OK) {
            sessionUri = request.getHeaderField("location")
        }
        Log.e(TAG, "Http request response code is ${request.responseCode}")
    }

    private fun resumeUploadFile(chunkStart: Long) {
        val url = URL(sessionUri)
        Log.e(TAG, "url = $url")
        var uploadedBytes: Long = 2                  //chunkSizeInMb * 1024 * 1024
        if (chunkStart + uploadedBytes > file.length()) {
            uploadedBytes = file.length() - chunkStart
        }
        Log.e(TAG, "file length == ${file.length()}")
        Log.e(TAG, "uploaded Bytes == $uploadedBytes")
        Log.e(TAG, "chunkStart == $chunkStart")

        val buffer = ByteArray(uploadedBytes.toInt())
        val fileInputStream = FileInputStream(file)
        fileInputStream.channel.position(chunkStart)
        var bytesRead = fileInputStream.read(buffer, 0, uploadedBytes.toInt())
        if (bytesRead == -1) Log.e(TAG, "no bytes loaded")
        Log.e(TAG, "bytesRead = $bytesRead")
        fileInputStream.close()


        Log.e(TAG, "(chunkStart + uploadedBytes) == ${chunkStart + uploadedBytes}")
        val request = url.openConnection() as HttpURLConnection
        request.apply {

            doOutput = true
            requestMethod = "PUT"

            connectTimeout = 10000
            setRequestProperty("Content-Length", "$uploadedBytes")
            setRequestProperty("Content-Range", "bytes $chunkStart-${chunkStart + uploadedBytes-1}/${file.length()}")
            setRequestProperty("Accept","*/*")
            outputStream.write(buffer)
            outputStream.close()
            //connect()
        }

        Log.e(TAG, "Content range ==  \"bytes $chunkStart-${chunkStart + uploadedBytes}/${file.length()}\"")
        Log.e(TAG, "RESUMABLE Http request response code is ${request.responseCode}")
        Log.e(TAG, "RESUMABLE body is ${readString(request.errorStream)}")

        val range: String = request.getHeaderField("range")
        chunkPosition = range.substring(range.lastIndexOf("-") + 1, range.length).toLong() + 1
    }

    fun readString(inputStream: InputStream): String {
        return String(inputStream.readBytes()) // Or whatever encoding
    }

    private fun createFile() {
        val createdFile = FileOutputStream(java.io.File("/sdcard/", "hello.txt")).apply { write("Hello World!".toByteArray())}
        val fileMetadata = File().apply {
            name = "hello.txt"
            parents = Collections.singletonList("1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l")
        }
    }


    private fun getCredentials(): GoogleCredential {
        Log.e(TAG, "inside getCredentials 1")

        val credFile = context.assets.open("credentials.json")

        return GoogleCredential.fromStream(credFile).createScoped(DriveScopes.all())
            //Collections.singletonList(DriveScopes.DRIVE))
    }
}


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