package com.example.recordingsystem.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

private const val TAG = "timeTextView"

class TimeTextView(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    var timeSec: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isFlashing = false
        set(value) {
            if (field != value) {
                field = value
                if (value) flashAnimation.enable() else flashAnimation.disable()
            }
        }

    private val painterBlack = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 250F
    }

    private val painterTransparent = Paint(painterBlack).apply {
        color = Color.TRANSPARENT
    }

    private var paint: Paint = painterBlack

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val timeString = timeToFormatString(timeSec)
        val bounds = Rect()
        paint.getTextBounds(timeString, 0, timeString.length, bounds)

        val textHeight = bounds.height().toFloat()
        val textWidth = paint.measureText(timeString)

        val x = (width - textWidth) / 2
        val y = 0F

        canvas?.translate(0F, textHeight)
        canvas?.drawText(timeString, x, y, paint)
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

    private val flashAnimation = object : Runnable {
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            paint = if (paint == painterBlack) painterTransparent else painterBlack
            invalidate()
            handler.postDelayed(this, 400L)
        }

        fun enable() {
            run()
        }

        fun disable() {
            handler.removeCallbacksAndMessages(null)
            paint = painterBlack
            invalidate()
        }
    }
}