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
import java.util.*

private val INSPIRATION = arrayOf(
    "'Passion is the secret ingredient that drives hard work and excellence.' -- Kelly Ayuote",
    "'Be someone who knows the way, goes the way and shows the way.' -- John C. Maxwell",
    "'Strive not to be a success, but rather to be of value.' -- Albert Einstein",
    "'Don’t find fault, find a remedy.' -- Henry Ford"
)

private const val TAG = "StatusIndicator"
private const val MILLIS_IN_MINUTE: Long = 60000
private const val FORGET_LAST_RECORDING_MINS: Long = 2*60
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
            var status: String

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
                state.fileSyncStatus != null && state.fileSyncStatus!!.error -> {
                    color = red
                    status = state.fileSyncStatus!!.message
                }
                else -> {
                    color = green
                    status = generateSuccessMessage()
                }
            }

            {
                val x = width / 2.toFloat()
                val y = height / 2.toFloat()
                val radius = 15.toFloat()
                it.drawCircle(x, y, radius, color);
            }()

            status = status.replace(".wav", "")
            rootView.statusTextView.text = status
        }
    }

    private fun generateSuccessMessage(): String {
        return when (state.recorderState) {
            RecordingService.RecorderState.IDLE -> {
                // IDLE
                state.fileSyncStatus?.let { fileSyncStatus ->
                    val ageOfStatusMessageMins = fileSyncStatus.date?.time?.let {
                        (Date().time - it) / MILLIS_IN_MINUTE
                    }
                    when (ageOfStatusMessageMins) {
                        null, 0L -> {
                            // Upload percent progress
                            fileSyncStatus.message
                        }
                        in 1..FORGET_LAST_RECORDING_MINS -> {
                            "${fileSyncStatus.message} ${prettyTime.format(fileSyncStatus.date)}"
                        }
                        else -> {
                            // It's been a while, display the message to get ready
                            null
                        }
                    }
                } ?: "Ready. Make sure you have water and lip balm"
            }
            else -> {
                // RECORDING, PAUSED
                val quoteIndex = ((state.timeWhenStarted?.time ?: 0) % INSPIRATION.size).toInt()
                INSPIRATION[quoteIndex]
            }
        }
    }
}
