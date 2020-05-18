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

class RecordingSystemActivity : AppCompatActivity(),
    RecordingService.ActivityCallbacks {
    private lateinit var serviceConnection: ServiceConnection
    private var service: RecordingService.API? = null
    private var noMicPopup: NoMicPopup? = null

    private val UIUpdater = object : Runnable {
        private var handler = Handler(Looper.getMainLooper())
        private var count = 0

        override fun run() {
            service?.let { s ->
                count++
                timeTextView.timeSec = Util.nanosToSec(s.getElapsedTime()) // Nanoseconds to seconds
                timeTextView.isFlashing = s.getState() == RecordingService.State.PAUSED

                statusIndicator.timeAgo = s.getTimeWhenStopped()
                //Log.e(TAG, "inside Runnable s.getTimeWhenStopped() ${s.getTimeWhenStopped()}")

                peakTextView.text = "$count -- ${s.getAudioPeek()}"
                soundVisualizer.volume = s.getAudioPeek()
                if (s.getAudioPeek() == Short.MAX_VALUE && s.getState() != RecordingService.State.IDLE) {
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

        UIUpdater.run()
        noMicPopup = NoMicPopup(window.decorView.rootView)
    }

    private fun startRecordingService() {
        Log.d(TAG, "inside startRecordingService()")

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceAPI: IBinder) {
                Log.d(TAG, "inside onServiceConnected")

                service = serviceAPI as RecordingService.API
                service?.registerActivity(this@RecordingSystemActivity) //Activity register in the service as client for callabcks!

                initUI()    // We have to wait to set onClickListeners until Service is connected.
                invalidate()
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Log.e(TAG, "ServiceConnection Disconnected")
                service = null
                Thread.sleep(1000L)
                startRecordingService()
            }
        }

        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(serviceIntent)
    }

    private fun initUI() {
        Log.d(TAG, "inside initUI()")
        service?.let{ s ->
            btnStart.setOnClickListener {
                s.toggleStartStop()
            }

            btnPause.setOnClickListener {
                s.togglePauseResume()
            }

            soundVisualizer.setOnClickListener {
                if (s.getState() == RecordingService.State.RECORDING && soundVisualizer.didClip)
                    soundVisualizer.didClip = false
            }
        }
    }

    override fun invalidate() {
        Log.d(TAG, "inside invalidate(), state = ${service?.getState()}")
        service?.let { s ->
            when (s.getState()) {
                RecordingService.State.IDLE -> {
                    btnStart.text = "Start"
                    btnPause.text = "Pause"
                    soundVisualizer.didClip = false
                }
                RecordingService.State.RECORDING -> {
                    btnStart.text = "Stop"
                    btnPause.text = "Pause"
                }
                RecordingService.State.PAUSED -> {
                    btnStart.text = "Stop"
                    btnPause.text = "Resume"
                }
            }

            val health = s.getHealth()
            statusIndicator.internet = health.internet
            statusIndicator.power = health.power
            statusIndicator.previousRecordingTime = Util.nanosToSec(health.recordingDuration)
            //statusIndicator.timeAgo = s.getTimeWhenStopped()
            //Log.e(TAG, "inside invalidate s.getTimeWhenStopped() ${s.getTimeWhenStopped()}")
            //noMicPopup?.isMicPresent = health.mic // Comment this line out if app needs to be tested on a Tablet without mic
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UIUpdater.stop()
        unbindService(serviceConnection)
    }

}
