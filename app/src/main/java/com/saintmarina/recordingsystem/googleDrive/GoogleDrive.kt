package com.saintmarina.recordingsystem.googleDrive

//import java.io.File
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


//https://drive.google.com/drive/folders/1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l?usp=sharing

private const val TAG = "Google Drive"
class ConnectionNotEstablished(message: String): Exception(message)
/*
 * service account token expiration time is 1 hour
 * Found in Drive Credentials source code
 * See source here https://github.com/googleapis/google-api-java-client/blob/master/google-api-client/src/main/java/com/google/api/client/googleapis/auth/oauth2/GoogleCredential.java#L387
 */
private const val TOKEN_EXPIRE_TIME_MILLI: Long = 60 * 60 * 1000

class GoogleDrive(credFile: InputStream) {
    private var  credential = GoogleCredential.fromStream(credFile).createScoped(DriveScopes.all())
    private val httpTransport = NetHttpTransport() //GoogleNetHttpTransport.newTrustedTransport()
    private var service =  Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
        .setApplicationName("Recording System")
        .build()
    private var refreshTokenThread = Thread {
        while (true) {
            try {
                credential.refreshToken()
                Log.i(TAG, "Token refreshed")
            } catch (e: Exception) {
                Log.d(TAG, "Exception caught while refreshing token. $e")
            }
            Thread.sleep(TOKEN_EXPIRE_TIME_MILLI/2)
        }
    }

    fun prepare() {
        refreshTokenThread.start()
    }

    fun openRequest(url: URL): HttpURLConnection {
        if (credential.accessToken == null)
            throw IllegalStateException("Access token not present")

        return (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer " + credential.accessToken)
        }
    }
}

        /*
var uploadedBytes: Long = chunkSize

if (chunkStart + uploadedBytes > fileSize) {
    uploadedBytes = fileSize - chunkStart
}

val buffer = ByteArray(uploadedBytes.toInt())
val fileInputStream = FileInputStream(file)
fileInputStream.channel.position(chunkStart)
val bytesRead = fileIS.read

if (bytesRead == -1) Log.e(TAG, "no bytes loaded")
fileInputStream.close()
 */

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