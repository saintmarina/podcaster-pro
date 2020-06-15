package com.saintmarina.recordingsystem.googleDrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val MILLIS_IN_SEC: Long = 1000
private const val TIMEOUT_AFTER_FAILURE: Long = 10 * MILLIS_IN_SEC


// TODO Figure out the way to distinguish from good and bad uploadStatus
// TODO make sure all UI elements are only touched from one thread.
class FilesSync(private val drive: GoogleDrive) {
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: (() -> Unit)? = null
    var uploadStatus: String = "" // TODO rewrite this message every where. Put the most appropriate message
        set(value) {
            field = value
            Log.i(TAG, value)
            onStatusChange?.invoke()
        }

    private fun updateUploadStatus(value: String) {
        uploadStatus = value
    }

    private val thread = Thread {
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                uploadStatus = "${job.file.name} upload successful"
            } catch (e: Exception) {
                uploadStatus = "${job.file.name} upload unsuccessful. Error: ${e.message}"
                Thread.sleep(TIMEOUT_AFTER_FAILURE)
                jobQueue.add(job)
            }
        }
    }

    init {
        thread.start()
    }

    fun scanForFiles() {  //done once at a boot time
        DESTINATIONS.forEach { dest ->
            File(dest.localDir).walk().forEach { f ->
                if (f.isFile  && f.name.endsWith(".wav"))
                    maybeUploadFile(f)
            }
        }
        Log.i(TAG, "FileSync initial scan finished")
    }

    fun maybeUploadFile(file: File) {
        jobQueue.add(GoogleDriveFile(file, drive, this::updateUploadStatus))
        Log.i(TAG, "$file added to upload job queue")
        return
    }
}
