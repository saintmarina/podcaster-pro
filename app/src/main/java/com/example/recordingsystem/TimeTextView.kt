package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

private const val TAG = "timeTextView"

class TimeTextView(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
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

    private val painterBlack = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 250F
    }

    private val painterTransparent = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 250F
    }

    var paint: Paint = painterBlack

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val timeString = timeToFormatString(time)
        val xPos = (width - paint.measureText(timeString)) / 2
        val yPos = 0F

        canvas?.translate(0F, 200F)
        canvas?.drawText(timeString, xPos.toFloat(), yPos, paint)
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
            paint = if (paint == painterBlack) painterTransparent else painterBlack
            handler.postDelayed(this, 400L)
            invalidate()
        }

        fun enable() {
            run()
        }

        fun disable() {
            handler.removeCallbacksAndMessages(null)
            paint = painterBlack
        }
    }
}