package com.saintmarina.recordingsystem.GoogleDrive

import android.util.Log
import java.io.File
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue

private const val TAG: String = "Files Sync"
private const val SEC_IN_MILLI = 1000L

class FileUploadJob(path: File, metadataObj: FileMetadata) {
    val file: File = path
    val metadata: FileMetadata = metadataObj
}

// Always think about this Thread as if you had 5 threads
class FilesSync(private val drive: GoogleDrive) {
    private val sdDir = File("/sdcard/Recordings/")
    private val jobQueue = LinkedBlockingQueue<FileUploadJob>()

    private val thread = Thread {
        while (true) {
            val job = jobQueue.take()
            var jobUploadFinished = false
            while (!jobUploadFinished) {
                Log.e(TAG, "inside the Thread upload while loop")
                try {
                    drive.uploadToDrive(job) // drive might be already trying to refresh its token. What's going to happen
                } catch (e: UploadUnsuccessfulException) {
                    Log.e(TAG, "Upload unsuccessful")
                    Thread.sleep(10 * SEC_IN_MILLI)
                    continue
                } catch (e: ConnectionNotEstablished) {
                    Log.e(TAG, "Unable to establish a connection")
                    Thread.sleep(10 * SEC_IN_MILLI)
                    continue
                }

                Log.e(TAG, "Upload successful")
                jobUploadFinished = true
            }
        }
    }.start()

    fun scanForFiles() {  //done one time at boot time
        for (file in sdDir.listFiles()) {
            if (!file.name.endsWith(".metadata.json"))
                maybeUploadFile(file)
        }
    }

    fun maybeUploadFile(audioFile: File) { //after the recording is done, call maybeUploadFile
        val metadataFile = File(sdDir, audioFile.name + ".metadata.json")

        val metadata =
            if (metadataFile.exists()) {
                FileMetadata.fromJsonFile(metadataFile)
            } else {
                FileMetadata()
            }

        if (!metadata.uploaded) {
            jobQueue.add(FileUploadJob(audioFile, metadata))
        }
    }
}



