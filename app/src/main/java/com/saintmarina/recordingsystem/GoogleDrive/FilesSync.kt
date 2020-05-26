package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val MILLIS_IN_SEC: Long = 1000
private const val JSON_EXT: String = ".metadata.json"
private const val TIMEOUT_AFTER_FAILURE: Long = 10 * MILLIS_IN_SEC

// Always think about this Thread as if you had 5 threads
class FilesSync(private val drive: GoogleDrive) {
    private val sdDir = File("/sdcard/Recordings/")
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: (() -> Unit)? = null
    var uploadStatus: String = "All recordings have been uploaded."
        set(value) {
            field = value
            Log.i(TAG, value)
            onStatusChange?.invoke()
        }

    private val thread = Thread {
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                job.metadata.uploaded = true
                job.metadata.serializeToJson(job.file)
                uploadStatus = "All files have been successfully uploaded."
            } catch (e: Exception) {
                uploadStatus = "Upload unsuccessful. ${e.message}"
                Thread.sleep(TIMEOUT_AFTER_FAILURE)
                jobQueue.add(job)
            }
        }
    }

    init {
        thread.start()
    }

    fun scanForFiles() {  //done once at a boot time
        sdDir.walk().forEach {
            if (it.isFile && it.name.endsWith(".wav")) {
                maybeUploadFile(it)
            }
        }
    }

    fun maybeUploadFile(audioFile: File) {
        Log.d(TAG, "AUDIO FILE ${audioFile.name}")
        val metadataFile = File(sdDir, audioFile.name + JSON_EXT)

        val metadata =
            if (metadataFile.exists()) {
                Log.d(TAG, "metadata exists")
                FileMetadata.deserializeFromJson(metadataFile)
            } else {
                FileMetadata()
            }
        Log.d(TAG, "is uploaded ${metadata.uploaded}")
        Log.d(TAG, "session ${metadata.sessionUrl}")
        if (!metadata.uploaded) {
            jobQueue.add(GoogleDriveFile(drive, audioFile, metadata, this))
        }
    }
}
