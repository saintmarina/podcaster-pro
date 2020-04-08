package com.example.recordingsystem

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*


const val BUF_SIZE = 1024*100
// Check for Internet
// Internet, Mic and power should go into a separate class. Like soundVisualizer class
// Fix Clipping bug. Don't clip when not recording
// Add max sound bar for the past two seconds
// Status should of mic, power and internet should be indicated with:
// green - when all three work
// flashing red  - when power or internet are not working
// warning message - when the mic is out. Stop recording before. Message should stay there until the mic is detected.
// add onPause/onResume lifecycle elements. App should preserve state and keep running in the background



class RecordingSystemActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    var recorder = AudioRecorder()
    private var outputFile: WavFileOutput? = null
    lateinit var mainHandler: Handler
    private var status = StatusChecker()
    var context: Context = this

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
            // Log.i("state", "TextView updated")
            powerTextView.text = "power = ${status.power.toString()}"
            micTextView.text = "mic = ${status.mic.toString()}"
            internetTextView.text = "internet = ${status.checkNetworkState(context)}"
            mainHandler.postDelayed(this, 30)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
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

        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(updateText)
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

    override fun onStart() {
        super.onStart()
        registerReceiver(status, status.getIntentFilter())
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStop() {
        super.onStop()
        recorder.close()

    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onRestart() {
        super.onRestart()
        recorder = AudioRecorder()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(status)
        mainHandler.removeCallbacks(updateText)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(status, status.getIntentFilter())
        mainHandler.post(updateText)
    }
}

