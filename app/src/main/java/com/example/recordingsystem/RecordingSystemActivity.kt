package com.example.recordingsystem

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet

// Case for tablet and a stand

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2



// Put StatusChecker inside Service

const val PEAK_REFRESH_DELAY: Long = 30

class RecordingSystemActivity : AppCompatActivity(), RecordingService.ActivityCallbacks {
    private var rService: RecordingService? = null
    private var noMicPopup: NoMicPopup? = null



    private var colorAnimation: ObjectAnimator? = null
    private lateinit var handler: Handler

    private val soundBarUpdater = object : Runnable {
        var count = 0
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            count++
            peakTextView.text = "$count -- ${rService?.recorder?.peak}"
            soundVisualizer.volume = rService?.recorder?.peak!!
            if (rService?.recorder?.peak == Short.MAX_VALUE && rService?.state != StateEnum.IDLE) {
                soundVisualizer.didClip = true
            }
            handler.postDelayed(this, PEAK_REFRESH_DELAY)
        }
    }

    // Lifecycle methods

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        startRecordingService()

        handler = Handler(Looper.getMainLooper())
        Log.e("XXX", "onCreate before noMicPopup")
        noMicPopup = NoMicPopup(window.decorView.rootView)
    }


    private fun startRecordingService() {
        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE)
        startService(serviceIntent)
        Log.e("XXX", "inside startForegroundService")
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.e("XXX", "inside mConnection")
            // Bind to LocalService, cast the IBinder and get LocalService instance
            val binder: RecordingService.LocalBinder = service as RecordingService.LocalBinder
            rService = binder.serviceInstance //Get instance of your service!
            Log.e("XXX", "inside mConnection fService = $rService")
            rService?.registerClient(this@RecordingSystemActivity) //Activity register in the service as client for callabcks!
            initUI()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Toast.makeText(this@RecordingSystemActivity, "onServiceDisconnected called", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun initUI() {
        rService?.let { service ->
            view_pager2.adapter = ViewPagerAdapter()
            view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentItem.text = (view_pager2.currentItem).toString()
                }
            })
            Log.e("XXX", "inside initUI after viewpager2")
            // Setting text for Buttons
            updateUI(service.state!!)

            // Setting Click Listener for Start Button
            btnStart.setOnClickListener {
                service.onStartClick()
            }

            // Setting Click Listener for Pause Button
            btnPause.setOnClickListener {
                service.onPauseClick()
            }
        }

    }

    override fun updateTime(time: Int) {
        timer.text = timeToFormatString(time)
    }

    override fun updateUI(state: StateEnum) {
        when (state) {
            StateEnum.IDLE -> {
                btnStart.text = "Start"
                btnPause.text = "Pause"
                soundVisualizer.didClip = false
            }
            StateEnum.RECORDING -> {
                btnStart.text = "Stop"
                btnPause.text = "Pause"
                if (colorAnimation != null && colorAnimation!!.isStarted)
                    stopFlashAnimation()

            }
            StateEnum.PAUSED -> {
                btnStart.text = "Stop"
                btnPause.text = "Resume"
                startFlashAnimation()
            }
        }
    }

    override fun updateStatus(
        internet: Boolean,
        power: Boolean,
        mic: Boolean,
        state: StateEnum
    ):Boolean {
        Log.e("XXX", "inside updateStatus in Activity")
        statusIndicator.internet = internet
        statusIndicator.power = power
       // noMicPopup?.isMicPresent = mic // Comment this line out if app needs to be tested on a Tablet without mic

        Log.e("XXX", "inside updateStatus in Activity, values set")
       return !noMicPopup!!.isMicPresent && state == StateEnum.RECORDING
    }

    private fun timeToFormatString(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / (60 * 60)

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    private fun startFlashAnimation() {
        colorAnimation = ObjectAnimator.ofInt(
            timer,
            "textColor",
            Color.BLACK,
            Color.TRANSPARENT).apply {
            duration = 600
            setEvaluator(ArgbEvaluator())
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopFlashAnimation() {
        colorAnimation!!.end()
        colorAnimation = null
        timer.setTextColor(Color.BLACK)
    }


}



/*

noMicPopup = NoMicPopup(window.decorView.rootView)
statusChecker.onChange = {
    statusIndicator.internet = it.internet
    statusIndicator.power = it.power

    //noMicPopup?.isMicPresent = it.mic // Comment this line out if app needs to be tested on a Tablet without mic
    if (!noMicPopup!!.isMicPresent && isRecording) {
        handleStop()
        Toast.makeText(this, "Recording was stopped. Connect the microphone and start again.", Toast.LENGTH_LONG).show()
    }
}

recorder = AudioRecorder()
chronoMeter = ChronoMeter(c_meter)
volumeBarTimer = Handler(Looper.getMainLooper())
soundEffect = SoundEffect(this)

*/



/* override fun onResume() {
     super.onResume()
     statusChecker.startMonitoring(this)
     volumeBarTimer.post(updateText)
 }

 override fun onPause() {
     super.onPause()
     statusChecker.stopMonitoring(this)
     volumeBarTimer.removeCallbacks(updateText)
 }

 override fun onDestroy() {
     super.onDestroy()
     soundEffect.releaseSoundEffects()
 }

 */

