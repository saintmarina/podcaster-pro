package com.example.recordingsystem

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet
// App should keep recording in the background, when killed. Possibly need background service.
// 3 hours max recording.

// Case for tablet and a stand
// Focusrite Dual with 48V button pressable in

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

const val MILLIS_DELAY: Long = 30

class RecordingSystemActivity : AppCompatActivity() {
    lateinit var recorder: AudioRecorder
    lateinit var visualizerTimer: Handler
    private var outputFile: WavFileOutput? = null
    private var statusChecker = StatusChecker()
    private var noMicPopup: NoMicPopup? = null
    private lateinit var soundEffect: SoundEffect

    private lateinit var chronoMeter: CMeter
    var timer = RecordingTimeOut()



    private var stopped = false
    private var pausePressed = false
    private var recordingStarted = false

    private val updateText = object : Runnable {
        var count = 0
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            count++
            peakTextView.text = "$count -- ${recorder.peak}"
            soundVisualizer.volume = recorder.peak
            if (recorder.peak == Short.MAX_VALUE && outputFile != null) {
                soundVisualizer.didClip = true
            }
            if (stopped) {
                if (chronoMeter?.maybeResetToZero()) stopped  = false
            }
            visualizerTimer.postDelayed(this, MILLIS_DELAY)
        }
    }


    private fun handleStart() { // Yes
        soundEffect.playStartSound()

        recordingStarted = true
        stopped = false
        chronoMeter.startChronometer()

        outputFile = WavFileOutput()
        recorder.outputFile = outputFile

        btnStart.text = "Stop"

        timer.startTimer({ handleStop() })
    }

    private fun handleStop() { // Yes
        soundEffect.playStopSound()

        chronoMeter.stopChronometer()
        stopped = true
        recordingStarted = false

        recorder.outputFile = null
        outputFile?.close()
        outputFile = null

        btnStart.text = "Start"
        soundVisualizer.didClip = false

        timer.stopTimer()
    }

    private fun handlePause() {
        soundEffect.playStopSound()

        chronoMeter.pauseChronometer()

        recorder.outputFile = null
        btnPause.text = "Resume"

        timer.stopTimer()
    }

    private fun handleResume() {
        soundEffect.playStartSound()

        chronoMeter.resumeChronometer()

        recorder.outputFile = outputFile
        btnPause.text = "Pause"

        timer.startTimer({ handleStop() }, chronoMeter.getCurTime())
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
        }

        btnPause.setOnClickListener {
            when {
                (outputFile != null && recordingStarted && !pausePressed) -> {
                    handlePause()
                    pausePressed = true
                }
                (outputFile != null && recordingStarted && pausePressed) -> {
                    handleResume()
                    pausePressed = false
                }
            }
        }

        noMicPopup = NoMicPopup(window.decorView.rootView)

        statusChecker.onChange = {
            statusIndicator.internet = it.internet
            statusIndicator.power = it.power

           // noMicPopup?.isMicPresent = it.mic // Comment this line out if app needs to be tested on a Tablet
            /* TODO Stop recording if !mic and we are recording */
        }

        chronoMeter = CMeter(c_meter)
        visualizerTimer = Handler(Looper.getMainLooper())
        soundEffect = SoundEffect(this)
        recorder = AudioRecorder()
    }


    override fun onResume() {
        super.onResume()
        statusChecker.startMonitoring(this)
        visualizerTimer.post(updateText)
    }

    override fun onPause() {
        super.onPause()
        statusChecker.stopMonitoring(this)
        visualizerTimer.removeCallbacks(updateText)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEffect.releaseSoundEffects()
    }
}

