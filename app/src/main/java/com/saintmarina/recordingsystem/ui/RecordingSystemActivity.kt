package com.saintmarina.recordingsystem.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.*
import com.saintmarina.recordingsystem.service.RecordingService
import com.saintmarina.recordingsystem.service.NANOS_IN_SEC
import com.saintmarina.recordingsystem.service.RecordingService.RecorderState
import kotlinx.android.synthetic.main.activity_recording_system.*
import java.lang.Float.max


/* UI:
All units are in pixels

Cards:
* they are positioned at X=274 (horizontally centered), Y=487
* they have a 30px rounded corner
* they have a 32px drop shadow at 70% opacity. If possible with an Y offset of 5px towards the bottom

Timer:
* The timer text color is #fafafa

Volume bar:
* The first volume rect is at X=110, Y=1295
* Volume rect appear every X=23 (so the second volume rect is at X=133px)
* 57 volume rect fill entirely the volume bar
* The volume bar clipping indicator is at X=105, Y=1284

Buttons:
* The record button is at X=650, Y=1625
* The pause button is at X=1110, Y=1676

Status:
* The status error indicator is at X=105, Y=2072
* The status message color is #e8e8e8

*/

/*
 * NICE TO HAVE:
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 * Add max sound bar for the past two seconds
 * Recovery files should have their header correctly set when uploaded
 * The audio feedback bipbip should seems delayed. It should not.
 *
 */
// MDM make sure it doesn't have the title bar
// TODO
// When the status is error: fade in an image
// When the sound viz is clipping, fade in an image.
//
// Have a background image, that flips back and forth between idle and recording screens, using TransitionDrawable
// Hide the cards gracefully when we start recording. Use TransitionDrawable
// Same for the timer. Use TransitionDrawable
//
// * Swipe up wallpaper

const val EXPERT_MODE = true

private const val UI_REFRESH_DELAY = 10L
private const val ACTIVITY_INVALIDATE_REFRESH_DELAY = 60000L /*1 minute*/
private const val VOLUME_BAR_SLOWDOWN_RATE_DB_PER_SEC = 100
private const val VOLUME_BAR_CLIP_DB = -1.0F
private const val DID_CLIP_TIMEOUT_MILLIS = 5000L

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

