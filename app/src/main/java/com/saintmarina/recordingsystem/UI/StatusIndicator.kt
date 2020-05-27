package com.saintmarina.recordingsystem.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.saintmarina.recordingsystem.Util
import kotlinx.android.synthetic.main.activity_recording_system.view.*
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class StatusIndicator(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val painterGreen = Paint().apply {
        color = Color.parseColor("#32CD32")
        isAntiAlias = true }
    private val painterRed = Paint().apply {
        color = Color.parseColor("#FF0000")
        isAntiAlias = true }
    private var prettyTime = PrettyTime()

    var power: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var internet: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var previousRecordingTime: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var timeAgo: Date? = null
        set(value) {
            field = value
            invalidate()
        }

    var fileSyncStatus: String = ""
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val x = width/2.toFloat()
            val y = height/2.toFloat()
            val radius = 15.toFloat()
            val paint: Paint
            val lastRecordingTime = if (timeAgo != null) prettyTime.format(timeAgo) else ""
            //Log.e("StatusIndicator", "lastRecordingTime = $lastRecordingTime")
            val previousRecording: String =
                if (previousRecordingTime == 0)
                    "No recording was previously made.$fileSyncStatus"
                else
                    "Previous recording was ${Util.formatAudioDuration(previousRecordingTime)} long $lastRecordingTime. $fileSyncStatus"

            when {
                !power -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Power is not connected."
                }
                !internet -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Internet is not connected."
                }
                else -> {
                    paint = painterGreen
                    rootView.statusTextView.text = "All good. $previousRecording"
                }
            }
            it.drawCircle(x, y, radius, paint);
        }
    }
}