package com.saintmarina.recordingsystem.UI

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.Service.RecordingService
import com.saintmarina.recordingsystem.Util
import kotlinx.android.synthetic.main.activity_recording_system.*

/*
 * TODO:
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 *
 * NICE TO HAVE:
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 *
 *
 */


const val UI_REFRESH_DELAY: Long = 30
private const val TAG: String = "RecordingActivity"

// Make sure that all the errors that I possibly see are shown to the UI
class RecordingSystemActivity : AppCompatActivity() {
    private lateinit var serviceConnection: ServiceConnection
    private var uiUpdater: UiUpdater? = null
    private var noMicPopup: NoMicPopup? = null

    // Lifecycle methods
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        Log.i(TAG, "inside onCreate of the Recording Activity")
        startRecordingService()
    }

    private fun handleServiceInvalidate(service: RecordingService.API) {
        Log.i(TAG, "Invalidating UI of the Activity")

        when (service.getState().recorderState) {
            RecordingService.RecorderState.IDLE -> {
                btnStart.text = "Start"
                btnPause.text = "Pause"
                soundVisualizer.didClip = false
                view_pager2.isUserInputEnabled = true
            }
            RecordingService.RecorderState.RECORDING -> {
                btnStart.text = "Stop"
                btnPause.text = "Pause"
                view_pager2.isUserInputEnabled = false
            }
            RecordingService.RecorderState.PAUSED -> {
                btnStart.text = "Stop"
                btnPause.text = "Resume"
                view_pager2.isUserInputEnabled = false
            }
        }

        val state = service.getState()
        statusIndicator.state = state
        //noMicPopup?.isMicPresent = state.micPlugged // Comment this line out if app needs to be tested on a Tablet without mic
    }

    private fun startRecordingService() {
        Log.i(TAG, "starting Recording Service")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceAPI: IBinder) {
                Log.i(TAG, "Service Connected. Initializing UI")
                val service = (serviceAPI as RecordingService.API)

                service.registerActivityInvalidate {
                    handleServiceInvalidate(service)
                }

                // initUI
                uiUpdater = UiUpdater(service).apply {
                    run()
                }

                noMicPopup = NoMicPopup(window.decorView.rootView)
                view_pager2.adapter = ViewPagerAdapter()
                view_pager2.currentItem = DESTINATIONS.indexOf(service.getDestination())
                view_pager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        currentItem.text = (view_pager2.currentItem).toString() // Take out before deploy
                        service.setDestination(DESTINATIONS[position])
                    }
                })

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
                Log.i(TAG, "ServiceConnection Disconnected")
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

    inner class UiUpdater(private val service: RecordingService.API): Runnable {
        private var handler = Handler(Looper.getMainLooper())
        private var count = 0

        override fun run() {
            service.let { s ->
                count++
                timeTextView.timeSec = Util.nanosToSec(s.getElapsedTime()) // Nanoseconds to seconds
                timeTextView.isFlashing = s.getState().recorderState == RecordingService.RecorderState.PAUSED
                statusIndicator.state.timeWhenStopped = s.getState().timeWhenStopped // TODO take out of this thread
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

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}

