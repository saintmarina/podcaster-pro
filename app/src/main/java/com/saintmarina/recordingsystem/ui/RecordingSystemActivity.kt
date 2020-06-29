package com.saintmarina.recordingsystem.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.R
import com.saintmarina.recordingsystem.service.RecordingService
import com.saintmarina.recordingsystem.Util
import com.saintmarina.recordingsystem.service.RecordingService.RecorderState
import kotlinx.android.synthetic.main.activity_recording_system.*


/* UI:
All units are in pixels

Cards are at X=274 (horizontally centered), Y=487

The first volume rect is at X=142, Y=1320
Volume rects appear every X=20 (so the second rect ise at X=162px)
65 rects fill entirely the volume bar
The volume bar clipping indicator is at X=105, Y=1284

The record button is at X=650, Y=1625
The pause button is at X=1110, Y=1676

The status error indicator is at X=105, Y=2072
 */

/*
 * NICE TO HAVE:
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 * Make sure that the sound recorded on software on the device is the same as recorded on tablet
 * Sound notification when recording time reached 2:45 hrs
 * Add max sound bar for the past two seconds
 * Card view instead of viewPager2
 *
 * Tell the user clipping occurred when stopping a recording if it occurred
 */
// MDM make sure it doesn't have the title bar
// TODO
// Buttons should be using https://developer.android.com/reference/android/widget/ImageButton
// https://developer.android.com/guide/topics/resources/drawable-resource#Transition
// startTransition(500) reverseTransition(500)
//
//
// For these two, use TransitionDrawable to transition between nothing, and an image
// When the status is error: fade in an image
// When the sound viz is clipping, fade in an image.
//
// Have a background image, that flips back and forth between idle and recording screens, using TransitionDrawable
// Hide the cards gracefully when we start recording. Use TransitionDrawable
// Same for the timer. Use TransitionDrawable
//
// * Swipe up wallpaper

//TODO fix the thread soundBar bug (onResume solution)


// Make sure that all the errors that I possibly see are shown to the UI
const val EXPERT_MODE: Boolean = true
const val UI_REFRESH_DELAY: Long = 30
const val ACTIVITY_INVALIDATE_REFRESH_DELAY: Long = 60000 /*1 minute*/
private const val TAG: String = "RecordingActivity"
private const val DID_CLIP_TIMEOUT_SECS = 5


class RecordingSystemActivity : Activity() {
    private lateinit var serviceConnection: ServiceConnection
    private var uiUpdater: UiUpdater? = null
    private var invalidateActivityTimer: InvalidateActivityTimer? = null
    private var noMicPopup: NoMicPopup? = null

    // Lifecycle methods
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_system)
        Log.i(TAG, "inside onCreate of the Recording Activity")
        startRecordingService()
    }

    override fun onResume() {
        super.onResume()
        /* This is to make full screen */
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        // TODO keep screen on only when we are recording
        // Try         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); if that doesn't work
        window.decorView.keepScreenOn = true
    }

    private fun handleServiceInvalidate(service: RecordingService.API) {
        this@RecordingSystemActivity.runOnUiThread {
            Log.i(TAG, "Invalidating UI of the Activity")
            val state = service.getState()
            statusIndicator.state = state

            when (state.recorderState) {
                RecorderState.IDLE -> {
                    btnStart.text = "Start"
                    btnPause.text = "Pause"
                    btnPause.isEnabled = false
                    soundVisualizer.didClip = false
                    destination_pager.isUserInputEnabled = true
                }
                RecorderState.RECORDING, RecorderState.PAUSED -> {
                    btnStart.text = "Stop"
                    btnPause.text = if (state.recorderState == RecorderState.RECORDING)
                                        "Pause"
                                    else
                                        "Resume"
                    btnPause.isEnabled = true
                    destination_pager.isUserInputEnabled = false
                }
            }

            btnStart.isEnabled = state.audioError == null
            btnPause.isEnabled = state.audioError == null

            if (!EXPERT_MODE) {
                noMicPopup?.isMicPresent = state.micPlugged  // Skipping noMic PopUp for EXPERT MODE
            }

        }
    }

    private fun startRecordingService() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Log.i(TAG, "starting Recording service")
        serviceConnection = object : ServiceConnection {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

                invalidateActivityTimer = InvalidateActivityTimer(service).apply {
                    run()
                }

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

                btnStart.setOnClickListener {
                    if (service.getState().recorderState == RecorderState.IDLE) {
                        Log.d(TAG, "supposed to fade in")
                        fade_background.alpha = 0f
                        fade_background.setImageResource(service.getDestination().imgPath)

                        fade_background.animate().apply {
                            cancel()
                            interpolator = AccelerateDecelerateInterpolator()
                            duration = 800
                            alpha(1f)
                            start()
                        }
                    } else {
                        Log.d(TAG, "supposed to fade out")
                        fade_background.animate().apply {
                            cancel()
                            interpolator = AccelerateDecelerateInterpolator()
                            duration = 800
                            alpha(0f)
                            start()
                        }
                    }
                    service.toggleStartStop()
                }

                btnPause.setOnClickListener {
                    service.togglePauseResume()
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


            /*
            imageView.startAnimation(fadeOut);

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    Animation fadeIn = AnimationUtils.loadAnimation(YourActivity.this, R.anim.fade_in);
                    imageView.startAnimation(fadeIn);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });*/
        }

        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(serviceIntent)
    }

    inner class UiUpdater(private val service: RecordingService.API): Runnable {
        private var handler = Handler(Looper.getMainLooper())
        private var whenClipped = 0L

        override fun run() {
            service.let { s ->
                updateTimer(s)

                if (s.getState().micPlugged) {
                    showSoundBar(s)
                } else {
                    hideSoundBar()
                }
            }
            handler.postDelayed(this, UI_REFRESH_DELAY)
        }

        fun stop() {
            handler.removeCallbacksAndMessages(null)
        }

        private fun updateTimer(service: RecordingService.API) {
            timeTextView.timeSec = Util.nanosToSec(service.getElapsedTime()) // Nanoseconds to seconds
            timeTextView.isFlashing = service.getState().recorderState == RecorderState.PAUSED
        }

        private fun showSoundBar(service: RecordingService.API) {
            service.resetAudioPeak()?.let { peak ->
                soundVisualizer.volume = peak
                if (peak >= 1.0) {
                    soundVisualizer.didClip = true
                    whenClipped = SystemClock.elapsedRealtimeNanos()
                }
            }
            whenClipped = maybeResetClipBar(whenClipped)
        }

        private fun hideSoundBar() {
            soundVisualizer.volume = 0F
        }

        private fun maybeResetClipBar(whenClipped: Long): Long {
            if (whenClipped != 0L)
                if (Util.nanosToSec(SystemClock.elapsedRealtimeNanos() - whenClipped) > DID_CLIP_TIMEOUT_SECS) {
                    soundVisualizer.didClip = false
                    return 0L
                }
            return whenClipped
        }
    }

    // Going to update Activity UI every minute
    // Purpose: keep StatusIndicator message up to date
    inner class InvalidateActivityTimer(private val service: RecordingService.API): Runnable {
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            Log.d(TAG, "INVALIDATE FROM TIMER")
            handleServiceInvalidate(service)
            handler.postDelayed(this, ACTIVITY_INVALIDATE_REFRESH_DELAY)
        }

        fun stop() {
            handler.removeCallbacksAndMessages(null)
        }

    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        invalidateActivityTimer?.stop()
        uiUpdater?.stop() // TODO start the thread onResume, and stop it onPause.
        super.onDestroy()
    }
}

