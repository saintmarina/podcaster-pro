package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Destination
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val MILLIS_IN_SEC: Long = 1000
private const val JSON_EXT: String = ".metadata.json"
private const val TIMEOUT_AFTER_FAILURE: Long = 10 * MILLIS_IN_SEC

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
                job.metadata.uploaded = true
                job.metadata.serializeToJson(job.file)
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
                if (f.isFile && f.name.endsWith(".wav")) {
                    maybeUploadFile(f)
                }
            }
        }
        Log.i(TAG, "FileSync initial scan finished")
    }

    fun maybeUploadFile(file: File) {
        val metadataFile = File(file.path + JSON_EXT)
        val metadata =
            if (metadataFile.exists() && !isMetadataCorrupted(metadataFile)) {
                FileMetadata.deserializeFromJson(metadataFile)
            } else {
                FileMetadata()
            }
        Log.i(TAG, "$file uploaded: ${metadata.uploaded}, session: ${metadata.sessionUrl}")
        if (!metadata.uploaded) {
            jobQueue.add(GoogleDriveFile(file, metadata, drive, this::updateUploadStatus))
            Log.i(TAG, "$file added to upload job queue")
            return
        }
        Log.i(TAG, "$file file was already uploaded")
    }

    private fun isMetadataCorrupted (file: File): Boolean {
        // All metadata files size is 219 bytes. If size is less than 150 bytes, it means that the file is corrupted and needs to be rewritten
        return file.length() < 150
    }
}
