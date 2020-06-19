package com.saintmarina.recordingsystem.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.service.RecordingService
import com.saintmarina.recordingsystem.Util
import kotlinx.android.synthetic.main.activity_recording_system.*

// TODO replace "uncomment for production" with a expertmode const on top.
// TODO fileStatus should be (String, Boolean), not (String, String)
// TODO make FileStatus constructor private
// TODO scanforfile -> use filter().
// TODO maybeUploadFile: use a one-liner for callback
// TODO statusBar - 100% should say "almost done"

// TODO grab WakeLock during recording

/*
 * TODO:
 * Make sure that the sound recorded on software and on the device is the same as recorded on tablet
 *
 * NICE TO HAVE:
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 * Check what happens to UI if an exception in raised in service onCreate // TODO talk to N the app crashes right away
 * Wake lock
 */

// TODO takeout clipping bar from the soundBar after ~5 seconds
// Make sure that all the errors that I possibly see are shown to the UI
const val UI_REFRESH_DELAY: Long = 30
private const val TAG: String = "RecordingActivity"


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
        val state = service.getState()
        statusIndicator.state = state
        this@RecordingSystemActivity.runOnUiThread {
            Log.i(TAG, "Invalidating UI of the Activity")

            when (service.getState().recorderState) {
                RecordingService.RecorderState.IDLE -> {
                    btnStart.text = "Start"
                    btnPause.text = "Pause"
                    btnPause.isEnabled = false
                    soundVisualizer.didClip = false
                    view_pager2.isUserInputEnabled = true
                }
                RecordingService.RecorderState.RECORDING -> {
                    btnStart.text = "Stop"
                    btnPause.text = "Pause"
                    btnPause.isEnabled = true
                    view_pager2.isUserInputEnabled = false
                }
                RecordingService.RecorderState.PAUSED -> {
                    btnStart.text = "Stop"
                    btnPause.text = "Resume"
                    btnPause.isEnabled = true
                    view_pager2.isUserInputEnabled = false
                }
            }
            //noMicPopup?.isMicPresent = state.micPlugged // Uncomment for production
        }
    }

    private fun startRecordingService() {
        Log.i(TAG, "starting Recording service")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceAPI: IBinder) {
                Log.i(TAG, "service Connected. Initializing UI")
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
                if (s.getState().micPlugged) {
                    peakTextView.text = "$count -- ${s.getAudioPeek()}"
                    soundVisualizer.volume = s.getAudioPeek()
                    if (s.getAudioPeek() == Short.MAX_VALUE && s.getState().recorderState != RecordingService.RecorderState.IDLE) {
                        soundVisualizer.didClip = true
                    }
                } else {
                    peakTextView.text = "$count -- ${0}"
                    soundVisualizer.volume = 0
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
