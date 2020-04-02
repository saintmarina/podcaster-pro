package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class SoundVisualizer (context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val painterGreen = Paint().apply { color = Color.parseColor("#32CD32") }
    private val painterYellow = Paint().apply { color = Color.parseColor("#FFFF00") }
    private val painterRed = Paint().apply { color = Color.parseColor("#FF0000") }

    var volume: Short = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val max = (width*volume)/Short.MAX_VALUE
            val spacer = 3
            val rectWidth = 7
            val fullRectWidth = spacer + rectWidth

            for (px in 0..max step fullRectWidth) {
                val rect = Rect(px, 0, px+rectWidth, height)

                val paintColor = when {
                    px > width*0.75 -> painterRed
                    px > width*0.5 -> painterYellow
                    else -> painterGreen
                }

                it.drawRect(rect, paintColor)
            }
        }

    }
}
