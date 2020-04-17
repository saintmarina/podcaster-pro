package com.example.recordingsystem

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
    private lateinit var recorder: AudioRecorder
    private lateinit var chronoMeter: ChronoMeter
    private lateinit var volumeBarTimer: Handler
    private lateinit var soundEffect: SoundEffect


    private var outputFile: WavFileOutput? = null
    private var statusChecker = StatusChecker()
    private var noMicPopup: NoMicPopup? = null

    private var isRecording = false
    private var pausePressed = false

    private val updateText = object : Runnable {
        var count = 0
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            count++
            peakTextView.text = "$count -- ${recorder!!.peak}"
            soundVisualizer.volume = recorder.peak
            if (recorder.peak == Short.MAX_VALUE && outputFile != null) {
                soundVisualizer.didClip = true
            }
            if (!isRecording) {
                if (chronoMeter.maybeResetToZero()) isRecording  = true
            }
            volumeBarTimer.postDelayed(this, MILLIS_DELAY)
        }
    }

    private fun handleStart() {
        isRecording = true
        soundEffect.playStartSound()

        chronoMeter.startChronometer { handleStop() }

        outputFile = WavFileOutput()
        recorder.outputFile = outputFile

        btnStart.text = "Stop"

        // Start Foreground Service. The app won't get killed while recording.
        val serviceIntent = Intent(this, ForegroundService::class.java)
        startService(serviceIntent)
    }

    private fun handleStop() {
        isRecording = false
        soundEffect.playStopSound()

        chronoMeter.stopChronometer()

        recorder.outputFile = null
        outputFile?.close()
        outputFile = null

        btnStart.text = "Start"
        soundVisualizer.didClip = false

        // Stop Foreground Service.
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }

    private fun handlePause() {
        pausePressed = true
        soundEffect.playStopSound()

        chronoMeter.pauseChronometer()

        recorder.outputFile = null
        btnPause.text = "Resume"
    }

    private fun handleResume() {
        pausePressed = false
        soundEffect.playStartSound()

        chronoMeter.resumeChronometer { handleStop() }

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
                (outputFile != null && isRecording && !pausePressed) ->
                    handlePause()
                (outputFile != null && isRecording && pausePressed) ->
                    handleResume()
            }
        }

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
    }


    override fun onResume() {
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
}

