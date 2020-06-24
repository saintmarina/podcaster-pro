package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.saintmarina.recordingsystem.service.RecordingService
import kotlinx.android.synthetic.main.activity_recording_system.view.*
import org.ocpsoft.prettytime.PrettyTime

private const val TAG = "StatusIndicator"

// TODO do not show .wav in the status message
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
                    // TODO Only say lastRecordingTime when more than 5 mins
                    // TODO show the water message if last recording time was more than 24hours
                    val lastRecordingTime = state.timeWhenStopped?.let { "Last recording made ${prettyTime.format(it)}" } ?: ""
                    status = state.fileSyncSyncStatus?.let { "${it.message}. $lastRecordingTime" } ?: "Ready. Make sure you have water and lip balm"
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

   // private fun
}
