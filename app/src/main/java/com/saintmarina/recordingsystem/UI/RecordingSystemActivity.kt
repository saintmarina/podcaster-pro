package com.saintmarina.recordingsystem.UI

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.GoogleDrive.GoogleDrive
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.Service.RecordingService
import com.saintmarina.recordingsystem.Util
import kotlinx.android.synthetic.main.activity_recording_system.*
import java.io.File
import java.security.AccessController.getContext

/*
 * TODO:
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 * Case for tablet and a stand
 *
 * NICE TO HAVE:
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 */


const val UI_REFRESH_DELAY: Long = 30
private const val TAG: String = "RecordingActivity"

class RecordingSystemActivity : AppCompatActivity() {
    private lateinit var serviceConnection: ServiceConnection
    private var uiUpdater: UiUpdater? = null
    private var noMicPopup: NoMicPopup? = null

    inner class UiUpdater(private val service: RecordingService.API ): Runnable {
        private var handler = Handler(Looper.getMainLooper())
        private var count = 0

        override fun run() {
            service.let { s ->
                count++
                timeTextView.timeSec = Util.nanosToSec(s.getElapsedTime()) // Nanoseconds to seconds
                timeTextView.isFlashing = s.getState().recorderState == RecordingService.RecorderState.PAUSED

                statusIndicator.timeAgo = s.getTimeWhenStopped()
                //Log.e(TAG, "inside Runnable s.getTimeWhenStopped() ${s.getTimeWhenStopped()}")

                peakTextView.text = "$count -- ${s.getAudioPeek()}"
                soundVisualizer.volume = s.getAudioPeek()
                if (s.getAudioPeek() == Short.MAX_VALUE && s.getState().recorderState != RecordingService.RecorderState.IDLE) {
                    soundVisualizer.didClip = true
                }
            }
            handler.postDelayed(this, UI_REFRESH_DELAY)
        }

        fun stop() {
            handler.removeCallbacksAndMessages(null)
        }
    }

    // Lifecycle methods
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        Log.d(TAG, "inside onCreate")
        startRecordingService()

        view_pager2.adapter = ViewPagerAdapter()
        view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItem.text = (view_pager2.currentItem).toString()
            }
        })

        uiUpdater?.run()
        noMicPopup = NoMicPopup(window.decorView.rootView)
    }

    private fun handleServiceInvalidate(service: RecordingService.API)
    {
        Log.d(TAG, "inside invalidate(), state = ${service.getState()}")

        when (service.getState().recorderState) {
            RecordingService.RecorderState.IDLE -> {
                btnStart.text = "Start"
                btnPause.text = "Pause"
                soundVisualizer.didClip = false
            }
            RecordingService.RecorderState.RECORDING -> {
                btnStart.text = "Stop"
                btnPause.text = "Pause"
            }
            RecordingService.RecorderState.PAUSED -> {
                btnStart.text = "Stop"
                btnPause.text = "Resume"
            }
        }

        val state = service.getState()
        statusIndicator.internet = state.internetAvailable
        statusIndicator.power = state.powerAvailable
        statusIndicator.previousRecordingTime = Util.nanosToSec(state.recordingDuration)
        statusIndicator.fileSyncStatus = state.fileSyncStatus
        //statusIndicator.timeAgo = s.getTimeWhenStopped()
        //Log.e(TAG, "inside invalidate s.getTimeWhenStopped() ${s.getTimeWhenStopped()}")
        //noMicPopup?.isMicPresent = state.micPlugged // Comment this line out if app needs to be tested on a Tablet without mic

    }

    private fun startRecordingService() {
        Log.d(TAG, "inside startRecordingService()")

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceAPI: IBinder) {
                Log.d(TAG, "inside onServiceConnected")
                val service = (serviceAPI as RecordingService.API)

                service.registerActivityInvalidate {
                    handleServiceInvalidate(service)
                }

                uiUpdater = UiUpdater(service).also { it.run() }

                // initUI
                btnStart.setOnClickListener {
                    service.toggleStartStop()
                }

                btnPause.setOnClickListener {
                    service.togglePauseResume()
                }

                soundVisualizer.setOnClickListener {
                    if (service.getState().recorderState == RecordingService.RecorderState.RECORDING && soundVisualizer.didClip)
                        soundVisualizer.didClip = false
                }

                handleServiceInvalidate(service)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Log.d(TAG, "ServiceConnection Disconnected")
                uiUpdater?.stop()
                uiUpdater = null
                Thread.sleep(1000L)
                startRecordingService()
            }
        }

        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(serviceIntent)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}

