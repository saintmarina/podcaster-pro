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
class FilesSync(private val drive: GoogleDrive) {
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: (() -> Unit)? = null
    var uploadStatus: Pair<String, Boolean> = Pair("", false) // TODO rewrite this message every where. Put the most appropriate message
        set(value) {
            field = value
            onStatusChange?.invoke()
        }

    private fun updateUploadStatus(value: Pair<String, Boolean>) {
        uploadStatus = value
    }

    private val thread = Thread {
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                uploadStatus = Pair("${job.file.name} uploaded", false)
            } catch (e: Exception) {
                uploadStatus = Pair("${job.file.name} upload unsuccessful.", true)
                Log.e(TAG, "Error: ${e.message}")
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
