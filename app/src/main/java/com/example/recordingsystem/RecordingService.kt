package com.example.recordingsystem

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


// Make sure that the sound recorded on software on the device is the same as recorded on tablet

// Case for tablet and a stand

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

const val FOREGROUND_ID = 1

class RecordingService(): Service() {
    var activity: ActivityCallbacks? = null
    lateinit var recorder: AudioRecorder
    private lateinit var handler: Handler
    private lateinit var soundEffect: SoundEffect
    private lateinit var timer: Timer
    private var statusChecker = StatusChecker()


    var outputFile: WavFileOutput? = null
    var time = 0
    var state: StateEnum? = null

    private val serviceRunnable = object : Runnable {

        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            time = timer.time
            activity?.updateTime(time)

            if (state == StateEnum.IDLE) {
                timer.maybeResetToZero()
            }
            handler.postDelayed(this, 1000)
        }
    }

    //returns the instance of the service
    private val mBinder: IBinder = LocalBinder()
    inner class LocalBinder : Binder() {
        val serviceInstance: RecordingService
            get() = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    // Here Activity register to the service as Callbacks client
    fun registerClient(act: Activity) {
        Log.e("XXX", "inside register client")
        activity = act as ActivityCallbacks
        Log.e("XXX", "Activity is set")
    }

    interface ActivityCallbacks {
        fun updateTime(time: Int)
        fun updateUI(state: StateEnum)
        fun updateStatus(
            internet: Boolean,
            power: Boolean,
            mic: Boolean,
            state: StateEnum) :Boolean
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()
        recorder = AudioRecorder()
        state = StateEnum.IDLE

        handler = Handler(Looper.getMainLooper())
        soundEffect = SoundEffect(this)
        timer = Timer()

        statusChecker.startMonitoring(this)

        statusChecker.onChange = {
            var micCameOut = if (activity != null)
                activity!!.updateStatus(it.internet, it.power, it.mic, state!!)
            else
                false
            Log.e("XXX", "inside statusChecker. internet = ${it.internet}, power = ${it.power}, mic = ${it.mic}")
            Log.e("XXX", "inside statusChecker, micCameOut = $micCameOut")
            if (micCameOut) {
                handleStop()
                Toast.makeText(
                    this,
                    "Recording was stopped. Connect the microphone and start again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            Log.e("XXX", "inside statusChecker, after IF")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("XXX", "inside onStart")
        handler.post(serviceRunnable)
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onStartClick() {
        Log.e("XXX", "Inside onStartClick")
        when (state) {
            StateEnum.IDLE -> {
                Log.e("XXX", "Inside onStartClick, IDLE -> handleStart")
                handleStart()
            }
            StateEnum.RECORDING -> {
                Log.e("XXX", "Inside onStartClick, RECORDING -> handleStart")
                handleStop()
            }
            StateEnum.PAUSED -> {
                handleStop()
            }
        }
    }

    fun onPauseClick() {
        Log.e("XXX", "Inside onPauseClick")
        when (state) {
            StateEnum.RECORDING -> {
                Log.e("XXX", "Inside onStartClick, RECORDING -> handlePause")
                handlePause()
            }
            StateEnum.PAUSED -> {
                Log.e("XXX", "Inside onStartClick, PAUSED -> handlePause")
                handleResume()
            }
            StateEnum.IDLE -> {
                Toast.makeText(
                    this,
                    "You are not recording.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun handleStart() {
        timer.startTimer()
        soundEffect.playStartSound()

        state = StateEnum.RECORDING
        activity?.updateUI(state!!)

        outputFile = WavFileOutput()
        recorder.outputFile = outputFile

         // Start Foreground Service.
         startForeground(FOREGROUND_ID, createNotification())
    }

    fun handleStop() {
        timer.stopTimer()
        soundEffect.playStopSound()

        state = StateEnum.IDLE
        activity?.updateUI(state!!)

        recorder.outputFile = null
        outputFile?.close()
        outputFile = null

        // Stop Foreground Service.
        stopForeground(true)
    }

     fun handlePause() {
         timer.pauseTimer()
         soundEffect.playStopSound()

         state = StateEnum.PAUSED
         activity?.updateUI(state!!)

         recorder.outputFile = null
    }

     fun handleResume() {
         timer.resumeTime()
         soundEffect.playStartSound()

         state = StateEnum.RECORDING
         activity?.updateUI(state!!)

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

    override fun onDestroy() {
        super.onDestroy()
        statusChecker.stopMonitoring(this)
    }


}
enum class StateEnum {
    IDLE,
    RECORDING,
    PAUSED
}