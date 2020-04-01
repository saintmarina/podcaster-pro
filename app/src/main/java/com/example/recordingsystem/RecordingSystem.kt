package com.example.recordingsystem

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_recording_system.*
import java.io.File
import java.io.IOException

class RecordingSystem : AppCompatActivity() {
    val recorder = AudioRecorder()
    lateinit var mainHandler: Handler

    private val updateText = object: Runnable {
        var count = 0;
        override fun run() {
            count++;
            peakTextView.text = "$count -- ${recorder.peak}"
            Log.i("state", "TextView updated")
            mainHandler.postDelayed(this, 30)
        }
    }

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
                mainHandler.removeCallbacks(updateText)
            }
            else {
                recorder.startRecording()
                btnStart.text = "Stop"
                mainHandler.post(updateText)
            }
        }
        mainHandler = Handler(Looper.getMainLooper())
    }
}
