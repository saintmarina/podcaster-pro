package com.saintmarina.recordingsystem.Service

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.api.client.http.FileContent
import com.saintmarina.recordingsystem.GoogleDrive.GoogleDrive
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.UI.RecordingSystemActivity
import com.saintmarina.recordingsystem.Util
import java.util.*
import java.util.concurrent.TimeUnit

// Make sure that the sound recorded on software on the device is the same as recorded on tablet

// Case for tablet and a stand

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

// Check what happens to UI if an exception in raised in service onCreate
// Wake lock

const val FOREGROUND_ID = 1
const val MAX_RECORDING_TIME_MILLIS: Long = 3 * 3600 * 1000
private const val TAG: String = "RecordingService"

@RequiresApi(Build.VERSION_CODES.Q)
class RecordingService(): Service() {
    enum class State {
        IDLE,
        RECORDING,
        PAUSED
    }

    private var state: State = State.IDLE
    private val health = API().Health()
    private var activity: ActivityCallbacks? = null
    private var statusChecker = StatusChecker()
    private var outputFile: WavFileOutput? = null
    private lateinit var recorder: AudioRecorder
    private lateinit var soundEffect: SoundEffect
    private var stopWatch: StopWatch = StopWatch()
    private var timeWhenStopped: Date? = null

    inner class API : Binder() {
        inner class Health {
            var internet: Boolean = true
            var mic: Boolean = true
            var power: Boolean = true
            var recordingDuration: Long = 0
        }

        fun getAudioPeek(): Short { return recorder.peak }
        fun getState(): State { return state }
        fun getElapsedTime(): Long { return stopWatch.getElapsedTimeNanos() }
        fun getHealth(): Health { return health }
        fun getTimeWhenStopped(): Date? {return timeWhenStopped}

        fun toggleStartStop() {
            Log.d(TAG, "inside onStartClick()")
            when (state) {
                State.IDLE -> start()
                State.RECORDING -> stop()
                State.PAUSED -> stop()
            }
        }

        fun togglePauseResume() {
            Log.d(TAG, "inside onPauseClick()")
            when (state) {
                State.IDLE -> showToast("You are not recording.")
                State.RECORDING -> pause()
                State.PAUSED -> resume()
            }
        }

        fun registerActivity(act: Activity) {
            Log.d(TAG, "inside registerActivity()")
            activity = act as ActivityCallbacks
        }
    }

    interface ActivityCallbacks {
        fun invalidate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return API()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "inside Service onCreate()")
        recorder = AudioRecorder()
        soundEffect = SoundEffect(this)
        statusChecker.startMonitoring(this)

        statusChecker.onChange = { status ->
            Log.d(TAG, "inside StatusChecker")
            health.apply {
                this.internet = status.internet
                this.power = status.power
                this.mic = status.mic
            }
            // The UI will display a large popup if mic is out
            if (!status.mic)
                stop()

            activity?.invalidate()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "inside Service onStartCommand()")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        statusChecker.stopMonitoring(this)
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
        if (state != State.IDLE)
            return

        Log.d(TAG, "inside handleStart()")
        stopWatch.reset() // Must be first in start() as other depend on the stopWatch.
        stopWatch.start()
        soundEffect.playStartSound()

        autoStopTimer.enable()

        state = State.RECORDING
        activity?.invalidate()

        outputFile = WavFileOutput()
        recorder.outputFile = outputFile

         // Start Foreground Service.
         startForeground(FOREGROUND_ID, createNotification())
    }


    private fun stop() {
        if (state == State.IDLE)
            return

        Log.d(TAG, "inside handleStop()")
        stopWatch.stop()
        soundEffect.playStopSound()

        autoStopTimer.disable()

        health.recordingDuration = stopWatch.getElapsedTimeNanos()
        stopWatch.reset()

        timeWhenStopped = Date()
        Log.e(TAG, "Service timeWhenStopped = $timeWhenStopped")
        state = State.IDLE
        activity?.invalidate()

       // var mediaContent: FileContent = FileContent("audio/wav", recorder.outputFile)

        recorder.outputFile = null
        outputFile?.close()
        outputFile = null



        // Stop Foreground Service.
        stopForeground(true)
    }

    private fun pause() {
        if (state != State.RECORDING)
            return

         Log.d(TAG, "inside handlePause()")
         stopWatch.stop()
         soundEffect.playStopSound()

         autoStopTimer.disable()

         state = State.PAUSED
         activity?.invalidate()

         recorder.outputFile = null
    }

     private fun resume() {
         if (state != State.PAUSED)
             return

         Log.d(TAG, "inside handleResume()")
         stopWatch.start()
         soundEffect.playStartSound()

         autoStopTimer.enable()

         state = State.RECORDING
         activity?.invalidate()

         recorder.outputFile = outputFile
    }

    private fun createNotification(): Notification {
        Log.d(TAG, "inside createNotification()")
        val pendingIntent = createContentIntent()
        val notification = NotificationCompat.Builder(applicationContext,
            CHANNEL_ID
        ).apply {
            setSmallIcon(R.drawable.ic_stat_name)
            setContentTitle("Recording system")
            setContentText("Currently recording, don't stop talking. Click on this notification to go back to the app.")
            priority = PRIORITY_HIGH
            setContentIntent(pendingIntent)
        }
        return notification.build()
    }

    private fun createContentIntent(): PendingIntent {
        val notificationIntent = Intent(applicationContext, RecordingSystemActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showToast( message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}