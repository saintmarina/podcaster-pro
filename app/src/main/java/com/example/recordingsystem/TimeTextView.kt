package com.example.recordingsystem

import android.R
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView


private const val TAG = "timeTextView"
private var colorAnimation: ObjectAnimator? = null

class TimeTextView(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val painterBlack = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 100F
    }

    private val painterTransparent = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 100F
    }

    var color: Paint = painterBlack

    var time: Int = 0
        set(value) {
            if (value != time) {
                field = value
                invalidate()
            }
        }

    var isFlashing = false
        set(value) {
            if (value != isFlashing) {
                field = value
                if (value) flash.enable() else flash.disable()
            }
        }



    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val timeString = timeToFormatString(time)
        Log.e(TAG, "inside onDraw, time is $timeString")
        canvas?.save();
        canvas?.translate(100F, 200F);
        canvas?.drawText(timeString, 0F, 0F, color);
    }


    private fun timeToFormatString(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / (60 * 60)

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    private val flash = object : Runnable {
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            color = if (color == painterBlack) painterTransparent else painterBlack
            handler.postDelayed(this, 400L)
            invalidate()
        }

        fun enable() {
            run()
        }

        fun disable() {
            handler.removeCallbacksAndMessages(null)
            color = painterBlack
        }
    }
}
/*
class TimeTextView(timeTextView: TextView, time: Int, state: RecordingService.State) {
    init {
        timeTextView.text = timeToFormatString(time)
        when (state) {
            RecordingService.State.IDLE ->
                stopTimerAnimation(timeTextView)
            RecordingService.State.RECORDING ->
                stopTimerAnimation(timeTextView)
            RecordingService.State.PAUSED ->
                startTimerAnimation(timeTextView)
        }
    }

    private fun timeToFormatString(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / (60 * 60)

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    private fun startTimerAnimation(textView: TextView) {
        Log.e(TAG, "start Animation")
        if (colorAnimation != null) return

        colorAnimation = ObjectAnimator.ofInt(
            textView,
            "textColor",
            Color.BLACK,
            Color.TRANSPARENT
        ).apply {
            duration = 600
            setEvaluator(ArgbEvaluator())
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopTimerAnimation(textView: TextView) {
        Log.e(TAG, "stop Animation")
        if (colorAnimation != null && colorAnimation!!.isStarted) {
            colorAnimation!!.end()
            colorAnimation = null
            textView.setTextColor(Color.BLACK)
        }
}
}*/