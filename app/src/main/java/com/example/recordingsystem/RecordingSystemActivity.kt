package com.example.recordingsystem

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


// Make sure that the sound recorded on software on the device is the same as recorded on tablet
// Status power and internet should be indicated with one indicator. Add text on the side when something is wrong
// App should keep recording in the background, when killed. Possibly need background service.
// Play sounds when start or stop recording
// Implement a timer. OnStop, stop the time and leave the time of a recording for 5 minutes. Make the timer blink onPause.
// 3 hours max recording.
// Order usbhub "Passthrough power"
// Case for tablet and a stand
// Focusrite Dual with 48V button pressable in

// Sound notification when recording time reached 2:45 hrs
// Add max sound bar for the past two seconds
// Card view instead of viewPager2

class RecordingSystemActivity : AppCompatActivity() {
    lateinit var recorder: AudioRecorder
    lateinit var visualizerTimer: Handler
    private var outputFile: WavFileOutput? = null
    private var statusChecker = StatusChecker()
    private var noMicPopup: NoMicPopup? = null

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
            visualizerTimer.postDelayed(this, 30)
        }
    }

    private fun handleStart() {
        outputFile = WavFileOutput()
        recorder.outputFile = outputFile
        btnStart.text = "Stop"
    }

    private fun handleStop() {
        recorder.outputFile = null
        outputFile?.close()
        outputFile = null

        btnStart.text = "Start"
        soundVisualizer.didClip = false
    }

    private fun handlePause() {
        recorder.outputFile = null
        btnPause.text = "Resume"
    }

    private fun handleResume() {
        recorder.outputFile = outputFile
        btnPause.text = "Pause"
    }

    // Lifecycle methods

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)

        view_pager2.adapter = ViewPagerAdapter()
        view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItem.setText((view_pager2.currentItem).toString())
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
            if (recorder.outputFile != null) {
                handlePause()
            } else {
                handleResume()
            }
        }

        noMicPopup = NoMicPopup(window.decorView.rootView)

        statusChecker.onChange = {
            statusIndicator.internet = it.internet
            statusIndicator.power = it.power

           // noMicPopup?.isMicPresent = it.mic
            /* TODO Stop recording if !mic and we are recording */
        }

        visualizerTimer = Handler(Looper.getMainLooper())
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
}

