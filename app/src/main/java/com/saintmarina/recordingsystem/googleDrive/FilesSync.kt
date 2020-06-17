package com.saintmarina.recordingsystem.googleDrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val TIMEOUT_AFTER_FAILURE: Long = 10000

class FileStatus(val success: String = "", val  error: String = "") {
    companion object {
        fun success(value: String): FileStatus {
            return FileStatus(value, "")
        }

        fun error(value: String): FileStatus {
            return FileStatus("", value)
        }
    }
}

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
            dest.localDir.walk().forEach() { f ->
                if (f.isFile && f.name.endsWith(".wav"))
                    maybeUploadFile(f)
            }
        }
        Log.i(TAG, "FileSync initial scan finished")
    }

    fun maybeUploadFile(file: File) {
        // the { uploadStatus = it } is for status callback
        // TODO GoogleDriveFile should not take the callback in its constructor, but rather, the callback should be set after object construction here, like we do in RecordingService with google drive.
        jobQueue.add(GoogleDriveFile(file, drive).apply {
            onStatusChange = { value ->
                uploadStatus = value
            }
        })
        Log.i(TAG, "$file added to upload job queue")

    }
}
