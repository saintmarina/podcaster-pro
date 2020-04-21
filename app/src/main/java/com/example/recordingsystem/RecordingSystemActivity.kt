package com.example.recordingsystem

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet
// App should keep recording in the background, when killed. Possibly need background service.

// Case for tablet and a stand
// Focusrite Dual with 48V button pressable in

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

class RecordingSystemActivity : AppCompatActivity(), ForegroundService.Callbacks {
    lateinit var fService: ForegroundService

    private var statusChecker = StatusChecker()
    private var noMicPopup: NoMicPopup? = null
    private var colorAnimation: ObjectAnimator? = null
    // Lifecycle methods

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        startForegroundService()

        noMicPopup = NoMicPopup(window.decorView.rootView)
        statusChecker.onChange = {
            statusIndicator.internet = it.internet
            statusIndicator.power = it.power

            noMicPopup?.isMicPresent = it.mic // Comment this line out if app needs to be tested on a Tablet without mic
            if (!noMicPopup!!.isMicPresent && fService.isRecording) {
                fService.handleStop()
                Toast.makeText(
                    this,
                    "Recording was stopped. Connect the microphone and start again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun startForegroundService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE)
        startService(serviceIntent)
        Log.e("XXX", "inside startForegroundService")
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.e("XXX", "inside mConnection")
            // Bind to LocalService, cast the IBinder and get LocalService instance
            val binder: ForegroundService.LocalBinder = service as ForegroundService.LocalBinder
            fService = binder.serviceInstance //Get instance of your service!
            Log.e("XXX", "inside mConnection fService = $fService")
            fService.registerClient(this@RecordingSystemActivity) //Activity register in the service as client for callabcks!
            initUI()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Toast.makeText(this@RecordingSystemActivity, "onServiceDisconnected called", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun initUI() {
        view_pager2.adapter = ViewPagerAdapter()
        view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItem.text = (view_pager2.currentItem).toString()
            }
        })
        Log.e("XXX", "inside initUI after viewpager2")
        Log.e("XXX", "inside initUI fService = $fService")
        // Setting text for Start Button
        if (fService.isRecording) {
            btnStart.text = "Stop"
        } else {
            btnStart.text = "Start"
        }

        // Setting text for Pause Button
        if (fService.pausePressed) {
            btnPause.text = "Resume"
        } else {
            btnPause.text = "Pause"
        }

        // Setting Click Listener for Start Button
        btnStart.setOnClickListener {
            if (fService.outputFile == null) {
                fService.handleStart()
                btnStart.text = "Stop"
            } else {
                fService.handleStop()
                if (colorAnimation != null && colorAnimation!!.isStarted)
                    stopFlashAnimation()
                btnStart.text = "Start"

                soundVisualizer.didClip = false
            }
        }

        // Setting Click Listener for Pause Button
        btnPause.setOnClickListener {
            when {
                (fService.outputFile != null && fService.isRecording && !fService.pausePressed) -> {
                    fService.handlePause()
                    startFlashAnimation()
                    btnPause.text = "Resume"
                }
                (fService.outputFile != null && fService.isRecording && fService.pausePressed) -> {
                    fService.handleResume()
                    stopFlashAnimation()
                    btnPause.text = "Pause"
                }
            }
        }
    }



    override fun updateClient(count: Int, peak: Short) {
        peakTextView.text = "$count -- $peak"
        soundVisualizer.volume = peak
        if (peak == Short.MAX_VALUE && fService.outputFile != null) {
            soundVisualizer.didClip = true
        }

        timer.text = timeToFormatString(fService.time)
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

