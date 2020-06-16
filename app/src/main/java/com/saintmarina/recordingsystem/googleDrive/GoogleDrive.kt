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
    private var credential = GoogleCredential.fromStream(credFile).createScoped(DriveScopes.all())
    private val httpTransport = NetHttpTransport() //GoogleNetHttpTransport.newTrustedTransport()
    private var _service =
        Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("Recording System")
            .build()
    private var refreshTokenThread = Thread {
        while (true) {
            try {
                credential.refreshToken()
                Log.i(TAG, "Token refreshed")
            } catch (e: Exception) {
                Log.i(TAG, "Exception caught while refreshing token. $e")
            }
            Thread.sleep(TOKEN_EXPIRE_TIME_MILLI / 2)
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