package com.saintmarina.recordingsystem.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.Destination
import com.saintmarina.recordingsystem.googledrive.FilesSync
import com.saintmarina.recordingsystem.googledrive.GoogleDrive
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.ui.RecordingSystemActivity
import com.saintmarina.recordingsystem.db.Database
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

const val FOREGROUND_ID = 1
private const val MAX_RECORDING_TIME_MILLIS: Long = 3 * 3600 * 1000
private const val TAG: String = "RecordingService"

@RequiresApi(Build.VERSION_CODES.Q)
class RecordingService: Service() {
    enum class RecorderState {
        IDLE,
        RECORDING,
        PAUSED
    }

    /* State is shared with the UI */
    class State {
        var recorderState: RecorderState = RecorderState.IDLE
        var internetAvailable: Boolean = true
        var micPlugged: Boolean = true
        var powerAvailable: Boolean = true
        var audioError: String? = null // Does not go back to being null
        var fileSyncSyncStatus: FileSyncStatus? = null
        var recordingDuration: Long = 0
        var timeWhenStopped: Date? = null
    }

    private val state = State()
    private var api: API = API()
    private var statusChecker = StatusChecker(this)
    private var outputFile: WavFileOutput? = null // We need this variable for setting fileOutput in pause() and resume()
    private lateinit var recorder: AudioRecorder
    private lateinit var soundEffect: SoundEffect
    private var stopWatch: StopWatch = StopWatch()
    private lateinit var fileSync: FilesSync
    private var destination = DESTINATIONS[0]

    inner class API : Binder() {
        var activityInvalidate: (() -> Unit)? = null
        fun getState(): State { return state }
        fun resetAudioPeak(): Float { return recorder.resetAudioPeak() }
        fun getElapsedTime(): Long { return stopWatch.getElapsedTimeNanos() }
        fun getDestination(): Destination { return destination }
        fun setDestination(dest: Destination) {
            if (dest == destination)
                return
            if (state.recorderState == RecorderState.RECORDING)
                throw Exception("Trying to change destination while recording")
            destination = dest
        }

        fun toggleStartStop() {
            Log.i(TAG, "toggleStartStop invoked")
            when (state.recorderState) {
                RecorderState.IDLE -> start()
                RecorderState.RECORDING -> stop()
                RecorderState.PAUSED -> stop()
            }
        }

        fun togglePauseResume() {
            Log.i(TAG, "togglePauseResume invoked")
            when (state.recorderState) {
                RecorderState.IDLE -> showToast("You are not recording")
                RecorderState.RECORDING -> pause()
                RecorderState.PAUSED -> resume()
            }
        }

        fun registerActivityInvalidate(cb: () -> Unit) {
            Log.i(TAG, "Registering the Recording Activity invalidate()")
            activityInvalidate = cb
        }
    }

    private fun invalidateActivity() {
        api.activityInvalidate?.invoke()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "onBind the API")
        return api
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnBind the API")
        api.activityInvalidate = null
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "inside onCreate of the RecordingService")
        Database.init(this)
        ensureRecordingDirsExist()

        recorder = AudioRecorder()
        recorder.onError = {
            state.audioError = it
            stop()
            invalidateActivity()
        }
        soundEffect = SoundEffect(this)
        statusChecker.startMonitoring()

        val drive = GoogleDrive(this.assets.open("credentials.json"))
            .also { it.prepare() }

        fileSync = FilesSync(drive)
        fileSync.onStatusChange = {
            state.fileSyncSyncStatus = it
            invalidateActivity()
        }

        statusChecker.onChange = {
            statusChecker.state = state
            if (!state.micPlugged) // The UI will display a large popup if mic is out
                stop()
            invalidateActivity()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        fileSync.scanForFiles()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
        statusChecker.stopMonitoring()
        soundEffect.releaseSoundEffects()
    }

    private val autoStopTimer = object : Runnable {
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            stop()
        }

        fun enable() {
            val deadline = MAX_RECORDING_TIME_MILLIS - TimeUnit.NANOSECONDS.toMillis(stopWatch.getElapsedTimeNanos())
            handler.postDelayed(this, deadline.coerceAtLeast(0))
        }

        fun disable() {
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun start() {
        if (state.recorderState != RecorderState.IDLE)
            return

        try {
            outputFile = WavFileOutput(destination.localDir)
            recorder.outputFile = outputFile
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            state.audioError = e.message
            return
        }

        stopWatch.reset() // Must be first in start() as other depend on the stopWatch.
        stopWatch.start()
        soundEffect.playStartSound()
        autoStopTimer.enable()

        state.recorderState = RecorderState.RECORDING
        invalidateActivity()

        startForeground(FOREGROUND_ID, createNotification())
        Log.i(TAG, "Audio recording started. Saving file to ${destination.localDir}")
    }

    private fun stop() {
        if (state.recorderState == RecorderState.IDLE)
            return

        stopWatch.stop()
        soundEffect.playStopSound()
        autoStopTimer.disable()
        state.timeWhenStopped = Date()

        state.recordingDuration = stopWatch.getElapsedTimeNanos()
        stopWatch.reset()

        state.recorderState = RecorderState.IDLE
        invalidateActivity()

        recorder.outputFile = null
        // Now we are sure that the AudioRecorder is no longer touching the file
        outputFile?.let {
            it.close() // Writes .wav header
            it.renameToDatedFile(state.recordingDuration)
            fileSync.maybeUploadFile(it.file, destination)
        }
        outputFile = null

        stopForeground(true)
        Log.i(TAG, "Audio recording stopped. File successfully recorded and passed to fileSync for upload.")
    }

    private fun pause() {
        if (state.recorderState != RecorderState.RECORDING)
            return

        stopWatch.stop()
        soundEffect.playStopSound()
        autoStopTimer.disable()

        state.recorderState = RecorderState.PAUSED
        invalidateActivity()

        recorder.outputFile = null
        Log.i(TAG, "Audio recording is paused.")
    }

     private fun resume() {
         if (state.recorderState != RecorderState.PAUSED)
             return

         stopWatch.start()
         soundEffect.playStartSound()
         autoStopTimer.enable()

         state.recorderState = RecorderState.RECORDING
         invalidateActivity()

         recorder.outputFile = outputFile
         Log.i(TAG, "Audio recording is resumed.")
    }

    private fun createNotification(): Notification {
        Log.i(TAG, "creating Notification for the Foreground service")
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_stat_name)
            setContentTitle("Recording system")
            setContentText("Currently recording, don't stop talking. Click on this notification to go back to the app.")
            priority = PRIORITY_HIGH
            setContentIntent(createPendingContentIntent())
        }.build()
    }

    private fun createPendingContentIntent(): PendingIntent {
        val notificationIntent = Intent(applicationContext, RecordingSystemActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun ensureRecordingDirsExist() {
        Log.i(TAG, "verify existence of DESTINATION local directories")
        DESTINATIONS.forEach { it.localDir.mkdirs() }
    }
}