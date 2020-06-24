package com.saintmarina.recordingsystem.googledrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Destination
import com.saintmarina.recordingsystem.service.FileSyncStatus
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val TIMEOUT_AFTER_FAILURE: Long = 10000

class FilesSync(private val drive: GoogleDrive): Thread() {
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: ((FileSyncStatus) -> Unit)? = null

    override fun run() {
        super.run()
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                onStatusChange?.invoke(FileSyncStatus.success("${job.file.name} uploaded"))
            } catch (e: Exception) {
                onStatusChange?.invoke(FileSyncStatus.error("${job.file.name} upload unsuccessful"))
                Log.e(TAG, "Error: ${e.message}")
                sleep(TIMEOUT_AFTER_FAILURE)
                jobQueue.add(job)
            }
        }
    }

    init {
        start()
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
        val job = GoogleDriveFile(file, dest, drive).apply {
            onStatusChange = this@FilesSync.onStatusChange
        }
        jobQueue.add(job)
        Log.i(TAG, "$file added to upload job queue")

    }
}
