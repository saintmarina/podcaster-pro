package com.saintmarina.recordingsystem.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.*
import com.saintmarina.recordingsystem.service.NANOS_IN_SEC
import com.saintmarina.recordingsystem.service.RecordingService
import com.saintmarina.recordingsystem.service.RecordingService.RecorderState
import kotlinx.android.synthetic.main.activity_recording_system.*
import java.awt.font.TextAttribute
import java.lang.Float.max
import kotlin.math.abs
import kotlin.math.pow

// After luanch:
// 3) Study power
// 7) investigate the two hubs, figure out the preference
// 8) Get shorter charge cable
// 9) Add expiration to FileSync
// 10) Know how to clean up the sdcard and the database
// 11) recordingsystemservice account change to saimaa

/*
 * NICE TO HAVE:
 * If FileSync has files to upload have a different message
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 * Add max sound bar for the past two seconds
 * Recovery files should have their header correctly set when uploaded
 * The audio feedback bipbip should seems delayed. It should not.
 * SaiMaa card should be properly shown
 * Save the destination in the Database
 * UI gradients seems to need some dithering
 * The app should have a lock screen
 * Get the logs
 * Delete old files here and there
 * Test google credential failures
 */

// TODO
// PUT A "TEST DRIVE" overlay on the cards when Destination is in test mode
// Don't say ready to record if the AudioRecorder has not initialized
//
// * Swipe up wallpaper

const val EXPERT_MODE = true

private const val UI_REFRESH_DELAY = 10L
private const val ACTIVITY_INVALIDATE_REFRESH_DELAY = 60000L /*1 minute*/
private const val VOLUME_BAR_SLOWDOWN_RATE_DB_PER_SEC = 100
private const val VOLUME_BAR_CLIP_DB = -1.0F
private const val DID_CLIP_TIMEOUT_MILLIS = 5000L
private const val PAGER_MARGIN_OFFSET = 470F
private const val PAGER_NEXT_CARD_SCALE = 0.8F

private const val TAG = "RecordingActivity"

@RequiresApi(Build.VERSION_CODES.P)
class RecordingSystemActivity : Activity() {
    private lateinit var serviceConnection: ServiceConnection
    private var uiUpdater: UiUpdater? = null
    private var noMicPopup: NoMicPopup? = null

    // Lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        Log.i(TAG, "onCreate of the Recording Activity")
        startRecordingService()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "$this onResume")

        uiUpdater?.reset()

        /* This is to make full screen */
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.keepScreenOn = true
    }


    private fun handleServiceInvalidate() {
        this@RecordingSystemActivity.runOnUiThread {
            uiUpdater?.invalidate()
        }
    }

    private fun startRecordingService() {
        Log.i(TAG, "starting Recording service")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceAPI: IBinder) {
                Log.i(TAG, "service Connected. Initializing UI")
                val service = (serviceAPI as RecordingService.API)

                service.registerActivityInvalidate {
                    handleServiceInvalidate()
                }

                uiUpdater = UiUpdater(service)

                noMicPopup = NoMicPopup(window.decorView.rootView)

                destination_pager.run {
                    adapter = ViewPagerAdapter()
                    offscreenPageLimit = 1
                    // Page transformer implements 'carousel' animation effect
                    setPageTransformer { page, position ->
                        page.translationX = -position * PAGER_MARGIN_OFFSET
                        page.scaleX = PAGER_NEXT_CARD_SCALE.pow(abs(position))
                        page.scaleY = page.scaleX
                        page.alpha = page.scaleX.pow(1.5f)
                    }
                    currentItem = DESTINATIONS.indexOf(service.getDestination())
                    registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            service.setDestination(DESTINATIONS[position])
                        }
                    })
                }

                btn_start.setOnClickListener {
                    service.toggleStartStop()
                }

                btn_pause.setOnClickListener {
                    service.togglePauseResume()
                }

                handleServiceInvalidate()
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

    inner class UiUpdater(private val service: RecordingService.API) {
        private val slowTimer = RepeatTimer(ACTIVITY_INVALIDATE_REFRESH_DELAY) { invalidate() }
        private val fastTimer = RepeatTimer(UI_REFRESH_DELAY) { fastInvalidate() }
        private val resetClippingTimer = OneshotTimer(DID_CLIP_TIMEOUT_MILLIS) {
            volume_clip_fader.show = false
        }
        private var lastFastFrameUpdate = 0L

        fun stop() {
            fastTimer.stop()
            slowTimer.stop()
        }

        fun reset() {
            fastTimer.reset()
            slowTimer.reset()
        }

        fun invalidate() {
            val state = service.getState()

            val status = StatusMessage.fromState(state)
            statusMessage.textAlignment = if (status.center) { View.TEXT_ALIGNMENT_CENTER } else { View.TEXT_ALIGNMENT_GRAVITY }
            statusMessage.text = status.message
            status_error_fader.show = status.isError

            when (state.recorderState) {
                RecorderState.IDLE -> {
                    // Keeps screed on the default state (not on) while not recording
                    // Note: has to be done on UI Thread
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    destination_pager.isUserInputEnabled = true
                    btn_pause.isEnabled = false

                    btn_rec_fader.show = false
                    background_recording_fader.show = false
                    destination_pager_fader.show = true
                    clock_fader.show = false
                }
                RecorderState.RECORDING, RecorderState.PAUSED -> {
                    // Keeps screen on while recording
                    // Note: has to be done on UI Thread
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    destination_pager.isUserInputEnabled = false
                    btn_pause.isEnabled = true // This needs a png to set, while it's disabled

                    btn_rec_fader.show = true
                    background_recording_fader.show = true
                    destination_pager_fader.show = false
                    clock_fader.show = true
                }
            }

            btn_pause_fader.show = state.recorderState == RecorderState.PAUSED

            btn_start.isEnabled = state.audioError == null
            btn_pause.isEnabled = state.audioError == null

            if (!EXPERT_MODE) {
                noMicPopup?.isMicPresent = state.micPlugged  // Skipping noMic PopUp for EXPERT MODE
            }

            fastInvalidate()
        }

        private fun fastInvalidate() {
            // This is executed every 30ms. It has to be fast!
            val now = SystemClock.elapsedRealtimeNanos()
            val nanosSinceLastUpdate = now - lastFastFrameUpdate

            updateTimer()
            updateSoundBar(nanosSinceLastUpdate)

            lastFastFrameUpdate = now
        }

        private fun updateTimer() {
            clock.timeSec = Util.nanosToSec(service.getElapsedTime()) // Nanoseconds to seconds
            clock.isFlashing = service.getState().recorderState == RecorderState.PAUSED
        }

        private fun updateSoundBar(nanosSinceLastUpdate: Long) {
            if (!service.getState().micPlugged) {
                soundVisualizer.volume = 0F
                volume_clip_fader.show = false
                return
            }

            service.resetAudioPeak()?.let { peak ->
                val volume = soundVisualizer.sampleToDb(peak)

                if (soundVisualizer.volume < volume)
                    soundVisualizer.volume = volume
                else {
                    SystemClock.elapsedRealtimeNanos()
                    soundVisualizer.volume = max(volume, soundVisualizer.volume -
                            VOLUME_BAR_SLOWDOWN_RATE_DB_PER_SEC * (nanosSinceLastUpdate.toFloat() / NANOS_IN_SEC))
                }

                if (volume >= VOLUME_BAR_CLIP_DB) {
                    volume_clip_fader.show = true
                    resetClippingTimer.reset()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        uiUpdater?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

