package com.saintmarina.recordingsystem.Service

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
import com.saintmarina.recordingsystem.Destination
import com.saintmarina.recordingsystem.GoogleDrive.FilesSync
import com.saintmarina.recordingsystem.GoogleDrive.GoogleDrive
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.UI.RecordingSystemActivity
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

// Make sure that the sound recorded on software on the device is the same as recorded on tablet

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

// Check what happens to UI if an exception in raised in service onCreate
// Wake lock

const val FOREGROUND_ID = 1
const val MAX_RECORDING_TIME_MILLIS: Long = 3 * 3600 * 1000
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
        var recorderState: RecorderState = RecorderState.IDLE //
        var internetAvailable: Boolean = true //
        var micPlugged: Boolean = true //
        var powerAvailable: Boolean = true //
        var audioError: String? = null
        var fileSyncStatus: String = ""
        var recordingDuration: Long = 0
        var timeWhenStopped: Date? = null
    }

    private val state = State()
    private var api: API = API()
    private var statusChecker = StatusChecker(this)
    private var outputFile: WavFileOutput? = null
    private lateinit var recorder: AudioRecorder
    private lateinit var soundEffect: SoundEffect
    private var stopWatch: StopWatch = StopWatch()
    private lateinit var fileSync: FilesSync
    private lateinit var destination: Destination

    inner class API : Binder() {
        var activityInvalidate: (() -> Unit)? = null
        fun getAudioPeek(): Short { return recorder.peak }
        fun getState(): State { return state }
        fun getElapsedTime(): Long { return stopWatch.getElapsedTimeNanos() }
        fun setDestination(dest: Destination) {
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
                RecorderState.IDLE -> showToast()
                RecorderState.RECORDING -> pause()
                RecorderState.PAUSED -> resume()
            }
        }

        fun registerActivityInvalidate(cb: () -> Unit) {
            Log.i(TAG, "Registering the Recording Acrivity invalidate()")
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
        Log.i(TAG, "inside onCreate of the Recording Service")
        recorder = AudioRecorder()
        soundEffect = SoundEffect(this)
        statusChecker.startMonitoring()

        val drive = GoogleDrive(this.assets.open("credentials.json"))
            .also { it.prepare() }

        fileSync = FilesSync(drive)

        fileSync.onStatusChange = {
            state.fileSyncStatus = fileSync.uploadStatus
            invalidateActivity()
        }

        Log.i(TAG, "fileSync onStatusChange callback assigned")
        statusChecker.onChange = {
            statusChecker.state = state

            // The UI will display a large popup if mic is out
            if (!state.micPlugged)
                stop()

            invalidateActivity()
        }
        Log.i(TAG, "StatusChecker onChange callback assigned")
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

        fileSync.maybeUploadFile(outputFile!!.file) //Upload file to Drive

        recorder.outputFile = null
        outputFile?.close()
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

        try {
            recorder.outputFile = null
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            state.audioError = e.message
        }
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

         try {
             recorder.outputFile = outputFile
         } catch (e: Exception) {
             Log.e(TAG, e.message.toString())
             state.audioError = e.message
         }
         Log.i(TAG, "Audio recording is resumed.")
    }

    private fun createNotification(): Notification {
        Log.i(TAG, "creating Notification for the Foreground Service")
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

    private fun showToast() {
        Toast.makeText(
            this,
            "You are not recording",
            Toast.LENGTH_SHORT
        ).show()
    }
}