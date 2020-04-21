package com.example.recordingsystem

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Chronometer
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet
// App should keep recording in the background, when killed. Possibly need background service.

// Case for tablet and a stand
// Focusrite Dual with 48V button pressable in

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2



const val MILLIS_DELAY: Long = 30
const val FOREGROUND_ID = 1

class ForegroundService(): Service() {
    var activity: Callbacks? = null
    private lateinit var recorder: AudioRecorder
    private lateinit var handler: Handler
    private lateinit var soundEffect: SoundEffect
    private lateinit var timer: Timer

    var outputFile: WavFileOutput? = null
    var isRecording = false
    var pausePressed = false
    var time = 0

    private val serviceRunnable = object : Runnable {
        var count = 0
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            count++
            activity!!.updateClient(count, recorder.peak)
            time = timer.time
            if (!isRecording && !pausePressed) {
                if (timer.maybeResetToZero()) isRecording  = true
            }
            handler.postDelayed(this, MILLIS_DELAY)
        }
    }

    //returns the instance of the service
    private val mBinder: IBinder = LocalBinder()
    inner class LocalBinder : Binder() {
        val serviceInstance: ForegroundService
            get() = this@ForegroundService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    //Here Activity register to the service as Callbacks client

    fun registerClient(act: Activity) {
        activity = act as Callbacks
        Log.e("XXX", "Activity is set")
    }

    interface Callbacks {
        fun updateClient(count: Int, peak: Short)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        recorder = AudioRecorder()
        handler = Handler(Looper.getMainLooper())
        soundEffect = SoundEffect(this)
        timer = Timer()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(serviceRunnable)
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun handleStart() {
        timer.startTimer()
        isRecording = true
        soundEffect.playStartSound()

        outputFile = WavFileOutput()
        recorder.outputFile = outputFile

         // Start Foreground Service.
         startForeground(FOREGROUND_ID, createNotification())
    }

    fun handleStop() {
        timer.stopTimer()
        isRecording = false
        soundEffect.playStopSound()

        recorder.outputFile = null
        outputFile?.close()
        outputFile = null

        // Stop Foreground Service.
        stopForeground(true)
    }

     fun handlePause() {
         timer.pauseTimer()
         pausePressed = true
         soundEffect.playStopSound()

         recorder.outputFile = null
    }

     fun handleResume() {
         timer.resumeTime()
         pausePressed = false
         soundEffect.playStartSound()

         recorder.outputFile = outputFile
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification(): Notification {
        val pendingIntent = createContentIntent()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
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
}