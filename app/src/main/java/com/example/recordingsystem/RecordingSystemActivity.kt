package com.example.recordingsystem

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_recording_system.*

const val BUF_SIZE = 1024*100

class RecordingSystemActivity : AppCompatActivity() {
    val recorder = AudioRecorder()


    lateinit var mainHandler: Handler

    private val updateText = object: Runnable {
        var count = 0
        override fun run() {
            count++
            peakTextView.text = "$count -- ${recorder.peak}"
            soundVisualizer.volume = recorder.peak
            if (recorder.peak == Short.MAX_VALUE) {soundVisualizer.didClip = true}
            // Log.i("state", "TextView updated")
            mainHandler.postDelayed(this, 30)
        }
    }

    /*private val updateMaxSoundBar = object: Runnable {
        var count = 0
        override fun run() {
            count++
            soundVisualizer.maxVolume = recorder.peak
            maxSoundTextView.text = "$count"
            mainHandler.postDelayed(this, 700)
        }
    }*/

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
            if (recorder.isRecording) {
                recorder.stop()
                btnStart.text = "Start"
                soundVisualizer.volume = 0
                if (soundVisualizer.didClip) {soundVisualizer.didClip = false}
                mainHandler.removeCallbacks(updateText)
                //mainHandler.removeCallbacks(updateMaxSoundBar)
            }
            else {
                recorder.startRecording()
                btnStart.text = "Stop"
                mainHandler.post(updateText)
               // mainHandler.post(updateMaxSoundBar)
            }
        }
        mainHandler = Handler(Looper.getMainLooper())
    }
}
