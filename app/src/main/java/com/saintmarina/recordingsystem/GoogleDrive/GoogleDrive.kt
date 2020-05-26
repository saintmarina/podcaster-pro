package com.saintmarina.recordingsystem.GoogleDrive

//import java.io.File
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import org.apache.http.auth.InvalidCredentialsException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


//https://drive.google.com/drive/folders/1y8LPodwpaPNI-BwGHyrbk5Ci7TEe0_0l?usp=sharing

private const val TAG = "Google Drive"
class ConnectionNotEstablished(message: String): Exception(message)
/*
 * Service account token expiration time is 1 hour
 * Found in Drive Credentials source code
 * See source here https://github.com/googleapis/google-api-java-client/blob/master/google-api-client/src/main/java/com/google/api/client/googleapis/auth/oauth2/GoogleCredential.java#L387
 */
private const val TOKEN_EXPIRE_TIME_MILLI: Long = 60 * 60 * 1000

class GoogleDrive(var credFile: InputStream) {
    private var  credential: GoogleCredential
    private val httpTransport: HttpTransport
    private var service: Drive
    private var refreshTokenThread: Thread


    // HOMEWORK: How to make sure that when we have a google drive and credentials are not initialized yet
    // Answer: to initialize all the variables in order we need. We make sure that credentials are initialized before they get used/
    init {
        httpTransport = NetHttpTransport() //GoogleNetHttpTransport.newTrustedTransport()
        credential = GoogleCredential.fromStream(credFile).createScoped(DriveScopes.all())
        service = Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("Recording System")
            .build()

        refreshTokenThread = Thread {
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