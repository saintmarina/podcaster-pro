package com.example.recordingsystem

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet
// App should keep recording in the background, when killed. Possibly need background service.

// Case for tablet and a stand
// Focusrite Dual with 48V button pressable in

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

const val MILLIS_DELAY: Long = 30

class RecordingSystemActivity : AppCompatActivity() {
    private var recorder: AudioRecorder? = null
    private var visualizerTimer: Handler? = null
    private var outputFile: WavFileOutput? = null
    private var statusChecker = StatusChecker()
    private var noMicPopup: NoMicPopup? = null
    private  var soundEffect: SoundEffect? = null

    private var notificationButtonClickReceiver: NotificationButtonClickReceiver? = null

    private var chronoMeter: CMeter? = null
    private var timer:RecordingTimeOut? = null



    private var isRecording = false
    private var pausePressed = false

    private val updateText = object : Runnable {
        var count = 0
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            count++
            peakTextView.text = "$count -- ${recorder!!.peak}"
            soundVisualizer.volume = recorder!!.peak
            if (recorder!!.peak == Short.MAX_VALUE && outputFile != null) {
                soundVisualizer.didClip = true
            }
            if (!isRecording) {
                if (chronoMeter!!.maybeResetToZero()) isRecording  = true
            }
            visualizerTimer!!.postDelayed(this, MILLIS_DELAY)
        }
    }

    private fun handleStart() {
        soundEffect!!.playStartSound()

        isRecording = true
        chronoMeter!!.startChronometer()

        outputFile = WavFileOutput()
        recorder!!.outputFile = outputFile

        btnStart.text = "Stop"

        timer!!.startTimer({ handleStop() })

        var serviceIntent = Intent(this, ForegroundService::class.java)
        startService(serviceIntent)

    }

    private fun handleStop() {
        soundEffect!!.playStopSound()

        chronoMeter!!.stopChronometer()
        isRecording = false

        recorder!!.outputFile = null
        outputFile?.close()
        outputFile = null

        btnStart.text = "Start"
        soundVisualizer.didClip = false

        timer!!.stopTimer()

        var serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }

    private fun handlePause() {
        soundEffect!!.playStopSound()

        chronoMeter!!.pauseChronometer()
        pausePressed = true

        recorder!!.outputFile = null
        btnPause.text = "Resume"

        timer!!.stopTimer()
    }

    private fun handleResume() {
        soundEffect!!.playStartSound()

        chronoMeter!!.resumeChronometer()
        pausePressed = false

        recorder!!.outputFile = outputFile
        btnPause.text = "Pause"

        timer!!.startTimer({ handleStop() }, chronoMeter!!.getCurTime())
    }

    // Lifecycle methods

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)

        view_pager2.adapter = ViewPagerAdapter()
        view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItem.text = (view_pager2.currentItem).toString()
            }
        })

        btnStart.setOnClickListener {
            if (outputFile == null) {
                handleStart()
            } else {
                handleStop()
            }
            var btnStartIntent = Intent(this, ForegroundService::class.java)
            btnStartIntent.putExtra("start_recording", "start_stop")
        }

        btnPause.setOnClickListener {
            when {
                (outputFile != null && isRecording && !pausePressed) ->
                    handlePause()
                (outputFile != null && isRecording && pausePressed) ->
                    handleResume()
            }
        }



       // notificationButtonClickReceiver.onChange = {

        //}

        noMicPopup = NoMicPopup(window.decorView.rootView)
        statusChecker.onChange = {
            statusIndicator.internet = it.internet
            statusIndicator.power = it.power

            //noMicPopup?.isMicPresent = it.mic // Comment this line out if app needs to be tested on a Tablet
            if (!noMicPopup!!.isMicPresent && isRecording) {
                handleStop()
            }
        }

        if (recorder == null) recorder = AudioRecorder()
        if (chronoMeter == null) chronoMeter = CMeter(c_meter)
        if (visualizerTimer == null) visualizerTimer = Handler(Looper.getMainLooper())
        if (soundEffect == null) soundEffect = SoundEffect(this)

        if (timer == null) timer = RecordingTimeOut()
    }


    override fun onResume() {
        super.onResume()
        statusChecker.startMonitoring(this)
        visualizerTimer!!.post(updateText)
    }

    override fun onPause() {
        super.onPause()
        statusChecker.stopMonitoring(this)
        visualizerTimer!!.removeCallbacks(updateText)
    }

    override fun onDestroy() {
        super.onDestroy()
      //restore   soundEffect.releaseSoundEffects()
    }
}

