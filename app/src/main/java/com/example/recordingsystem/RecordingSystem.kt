package com.example.recordingsystem

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_recording_system.*
import java.io.File
import java.io.IOException

class RecordingSystem : AppCompatActivity() {
    private var output: File? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false
    private var recordingPaused: Boolean = false


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)

        view_pager2.adapter = ViewPagerAdapter()
        view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItem.setText((view_pager2.currentItem).toString())
            }
        })

        Log.i("State", "ViewPager is done")

        output = File("/sdcard/test1.mp4a")

        @TargetApi(Build.VERSION_CODES.O)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setAudioSamplingRate(48000);
            setAudioEncodingBitRate(128000);
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(output)
        }

        Log.i("State", "Creating mediaRecorder DONE")

        button_start_recording.setOnClickListener {
            startOrStopRecording()
        }

        button_pause_recording.setOnClickListener {
            pauseResumeRecording()
        }
    }

    private fun startOrStopRecording() {
        if (!isRecording) {
            mediaRecorder?.apply {
                prepare()
                start()
                isRecording = true
                button_start_recording.text = "Stop"
            }
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } else {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            button_start_recording.text = "Start"
            Toast.makeText(this, "Stopped!", Toast.LENGTH_SHORT).show()

        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun pauseResumeRecording() {
        if(isRecording) {
            if(!recordingPaused){
                mediaRecorder?.pause()
                recordingPaused = true
                button_pause_recording.text = "Resume"
                Toast.makeText(this,"Paused!", Toast.LENGTH_SHORT).show()
            }else{
                mediaRecorder?.resume()
                button_pause_recording.text = "Pause"
                recordingPaused = false
                Toast.makeText(this,"Resume!", Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(this,"You are not recording", Toast.LENGTH_SHORT).show()
    }
}
