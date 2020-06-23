package com.saintmarina.recordingsystem.googledrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Destination
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val TIMEOUT_AFTER_FAILURE: Long = 10000

class FileStatus private constructor(val message: String = "", val  error: Boolean = false) {
    fun getStatusMessage(): String {
        return message
    }

    fun errorOccurred(): Boolean {
        return error
    }

    companion object {
        fun success(value: String): FileStatus {
            return FileStatus(value, false)
        }

        fun error(value: String): FileStatus {
            return FileStatus(value, true)
        }
    }
}

// TODO Make it a Thread()
class FilesSync(private val drive: GoogleDrive) {
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: (() -> Unit)? = null
    var uploadStatus: FileStatus = FileStatus.success("")
        set(value) {
            field = value
            onStatusChange?.invoke()
        }

    private val thread = Thread {
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                uploadStatus = FileStatus.success("${job.file.name} uploaded")
            } catch (e: Exception) {
                uploadStatus = FileStatus.error("${job.file.name} upload unsuccessful")
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
            dest.localDir.walk()
                .filter { it.isFile }
                .forEach { maybeUploadFile(it, dest) }
        }
        Log.i(TAG, "FileSync initial scan finished")
    }

    fun maybeUploadFile(file: File, dest: Destination) {
        jobQueue.add(GoogleDriveFile(file, dest, drive).apply { onStatusChange = { uploadStatus = it } })
        Log.i(TAG, "$file added to upload job queue")

    }
}
