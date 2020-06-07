package com.saintmarina.recordingsystem.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.saintmarina.recordingsystem.Service.RecordingService
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

    var state: RecordingService.State = RecordingService.State()
        set(value) {
            field = value
            invalidate()
        }
    // TODO instead of having all these variable pass State object from recording service

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val x = width/2.toFloat()
            val y = height/2.toFloat()
            val radius = 15.toFloat()
            val paint: Paint
            val lastRecordingTime = if (state.timeWhenStopped != null) prettyTime.format(state.timeWhenStopped) else ""
            val previousRecording: String = if (state.recordingDuration == 0L) ""
                else "Previous recording was ${Util.formatAudioDuration(Util.nanosToSec(state.recordingDuration))} long $lastRecordingTime"

            // TODO check for all items in the State object. Think about how to make the app pleasant to interact with. User should be informed and the expectations of the user should be managed
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
                state.recorderState == RecordingService.RecorderState.RECORDING -> {
                    paint = painterGreen
                    rootView.statusTextView.text = "All good. Currently recording"
                }
                state.audioError != null -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Contact the developer.Error occured: ${state.audioError}"
                }
                else -> {
                    paint = painterGreen
                    rootView.statusTextView.text = "All good ${state.fileSyncStatus} ${previousRecording}"
                }
            }
            it.drawCircle(x, y, radius, paint);
        }
    }
}