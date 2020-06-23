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

class StatusIndicator(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val painterGreen = Paint().apply {
        color = Color.parseColor("#32CD32")
        isAntiAlias = true }
    private val painterRed = Paint().apply {
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
            val x = width/2.toFloat()
            val y = height/2.toFloat()
            val radius = 15.toFloat()
            val paint: Paint

            when {
                !state.micPlugged -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Microphone is not connected"
                }
                !state.powerAvailable -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Power is not connected"
                }
                !state.internetAvailable -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Internet is not connected"
                }
                state.audioError != null -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Contact Nico at 646-504-6464. Error occurred: ${state.audioError}"
                }
                state.fileSyncSyncStatus != null && state.fileSyncSyncStatus!!.errorOccurred() -> {
                    paint = painterRed
                    rootView.statusTextView.text = "${state.fileSyncSyncStatus!!.getStatusMessage()}. Retrying..."
                }
                else -> {
                    val lastRecordingTime = if (state.timeWhenStopped != null) "Last recording made ${prettyTime.format(state.timeWhenStopped)}" else ""
                    val status = state.fileSyncSyncStatus?.let {
                        if (it.getStatusMessage().isEmpty())
                            "Ready."
                        else "${it.getStatusMessage()}. $lastRecordingTime"
                    } ?: "Ready."
                    paint = painterGreen
                    rootView.statusTextView.text = "$status"
                }
            }
            it.drawCircle(x, y, radius, paint);
        }
    }
}
