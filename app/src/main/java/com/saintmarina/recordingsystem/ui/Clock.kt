package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.saintmarina.recordingsystem.RepeatTimer
import com.saintmarina.recordingsystem.Util

private const val TEXT_COLOR = "#fafafa"
private const val PAUSE_BLINK_DELAY = 600L

class Clock(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val paint = Paint().apply {
        color = Color.parseColor(TEXT_COLOR)
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 280F
        setShadowLayer(12f, 0f, 5f, Color.argb(120, 0, 0, 0))
    }

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
                if (value) {
                    hideText = true
                    pauseAnimation.reset()
                } else {
                    pauseAnimation.stop()
                    hideText = false
                }
                invalidate()
            }
        }

    private var hideText = false
    private val pauseAnimation = RepeatTimer(PAUSE_BLINK_DELAY) {
        hideText = !hideText
        invalidate()
    }.apply { stop() } // TODO Repeat timer shouldn't be scheduled right away

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (hideText)
            return

        val timeString = Util.formatAudioDuration(timeSec)
        val bounds = Rect()
        paint.getTextBounds(timeString, 0, timeString.length, bounds)

        val textHeight = bounds.height().toFloat()
        val textWidth = paint.measureText(timeString)

        val x = (width - textWidth) / 2
        val y = 0F

        canvas?.translate(0F, textHeight)
        canvas?.drawText(timeString, x, y, paint)
    }
}