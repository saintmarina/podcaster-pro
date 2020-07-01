package com.saintmarina.recordingsystem.googledrive

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Destination
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val TIMEOUT_AFTER_FAILURE_MILLIS: Long = 10000

class FilesSync(private val drive: GoogleDrive, val context: Context): Thread() {
    private val jobQueue = LinkedBlockingQueue<GoogleDriveFile>()
    var onStatusChange: ((FileSyncStatus) -> Unit)? = null
    private val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RecordingSystem::Wakelock")
    }

    override fun run() {
        super.run()
        while (true) {
            val job = jobQueue.take()
            try {
                job.upload()
                wakeLock.release()
            } catch (e: Exception) {
                job.reportErrorStatus("failed to upload: ${e.message}. Retrying")
                Log.e(TAG, "Error: $e")
                sleep(TIMEOUT_AFTER_FAILURE_MILLIS)
                jobQueue.add(job)
            }
        }
    }

    init {
        start()
        scanForFiles()
    }

    private fun scanForFiles() {  //done once at a boot time
        DESTINATIONS.forEach { dest ->
            dest.localDir.walk()
                .filter { it.isFile }
                .forEach { addJob(makeJob(it, dest)) }
        }
        Log.i(TAG, "FileSync initial scan finished")
    }

    fun makeJob(file: File, dest: Destination): GoogleDriveFile {
        return GoogleDriveFile(file, dest, drive).apply {
            onStatusChange = this@FilesSync.onStatusChange
        }
    }

    fun addJob(job: GoogleDriveFile) {
        wakeLock.acquire()
        jobQueue.add(job)
        Log.i(TAG, "$job added to upload job queue")
    }
}
