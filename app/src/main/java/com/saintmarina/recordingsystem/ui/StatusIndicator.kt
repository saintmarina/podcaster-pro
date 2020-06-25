package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.saintmarina.recordingsystem.service.RecordingService
import kotlinx.android.synthetic.main.activity_recording_system.view.*
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

private val INSPIRATION = arrayOf(
    // Before start

    // After start
    "'Passion is the secret ingredient that drives hard work and excellence.' -- Kelly Ayuote",
    "'Be someone who knows the way, goes the way and shows the way.' -- John C. Maxwell",
    "'Strive not to be a success, but rather to be of value.' -- Albert Einstein",
    "'Don’t find fault, find a remedy.' -- Henry Ford"
)

private const val TAG = "StatusIndicator"
private const val MINUTE_IN_MILLIS: Long = 60000
private const val DAY_IN_MINS: Int = 1440

// TODO When starting recording, display an empowering message from a random list (5 messages)
// TODO grab the wake lock only when recording

class StatusIndicator(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val green = Paint().apply {
        color = Color.parseColor("#32CD32")
        isAntiAlias = true }
    private val red = Paint().apply {
        color = Color.parseColor("#FF0000")
        isAntiAlias = true }
    private var prettyTime = PrettyTime()

    var state: RecordingService.State = RecordingService.State()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let { it ->
            val color: Paint
            val status: String

            when {
                state.audioError != null -> {
                    color = red
                    status = "Contact Nico at +1-646-504-6464. Audio failure: ${state.audioError}"
                }
                !state.micPlugged -> {
                    color = red
                    status = "Microphone is not connected"
                }
                !state.internetAvailable -> {
                    color = red
                    status = "Internet is not connected"
                }
                !state.powerAvailable -> {
                    color = red
                    status = "Power is not connected"
                }
                state.fileSyncSyncStatus != null && state.fileSyncSyncStatus!!.error -> {
                    color = red
                    status = "${state.fileSyncSyncStatus!!.message}. Retrying..."
                }
                else -> {
                    color = green
                    status = generateStatusMessage()
                }
            }

            {
                val x = width / 2.toFloat()
                val y = height / 2.toFloat()
                val radius = 15.toFloat()
                it.drawCircle(x, y, radius, color);
            }()
            rootView.statusTextView.text = status.replace(".wav", "")
        }
    }

    private fun lastRecordingTimeMoreThan(minutes: Int, timeWhenStopped: Date): Boolean {
        Log.d(TAG, "Date().time == ${Date().time}, timeWhenStopped == $timeWhenStopped")
        Log.d(TAG, "result = ${Date().time - timeWhenStopped.time}")
        return Date().time - timeWhenStopped.time > minutes * MINUTE_IN_MILLIS
    }

    // TODO Only say lastRecordingTime when more than 5 mins
    // TODO show the water message if last recording time was more than 24hours
    private fun generateStatusMessage(): String {
        // Show last recording time only after 5 minutes after the recording
        // Show empty String meanwhile
        val lastRecordingTime = state.timeWhenStopped?.let {
            if (lastRecordingTimeMoreThan(5, it)) {
                "Last recording made ${prettyTime.format(it)}"
            } else ""
        } ?: ""

        // Show information about previous recording for the first 24 hours after the recording
        // If it has been 24 hours since last recording, display the message to get ready
        val getReady = "Ready. Make sure you have water and lip balm"
        val messageBeforeRecording = state.timeWhenStopped?.let {
            if (!lastRecordingTimeMoreThan(DAY_IN_MINS, it)) {
                state.fileSyncSyncStatus?.let {fs-> "${fs.message}. $lastRecordingTime"}
            } else getReady
        } ?: getReady

        // Random inspirational quotes
        val messageAfterRecordingStart = INSPIRATION[Random().nextInt(INSPIRATION.size)]

        return if (state.recorderState == RecordingService.RecorderState.IDLE)
                    messageBeforeRecording
               else messageAfterRecordingStart

    }
}
