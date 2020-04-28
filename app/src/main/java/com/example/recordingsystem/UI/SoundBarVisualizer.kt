package com.example.recordingsystem.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.math.ln
import kotlin.math.roundToInt

const val MIN_DB = -50.0

class SoundVisualizer (context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val painterGreen = Paint().apply { color = Color.parseColor("#32CD32") }
    private val painterYellow = Paint().apply { color = Color.parseColor("#888800") }
    private val painterRed = Paint().apply { color = Color.parseColor("#FF0000") }

    var didClip: Boolean = false
    var volume: Short = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val maxPx = DbToPx(sampleToDb(volume))
            val spacer = 3
            val rectWidth = 7
            val fullRectWidth = spacer + rectWidth
            val clipIndicator = Rect(width-11, 0, width, height)

            for (px in 0..maxPx step fullRectWidth) {
                val rect = Rect(px, 0, px+rectWidth, height)

                val paintColor = when {
                    px > width*0.75 -> painterRed
                    px > width*0.5 -> painterYellow
                    else -> painterGreen
                }

                it.drawRect(rect, paintColor)
            }

            if (didClip) {
                it.drawRect(clipIndicator, painterRed)
            }
        }
    }

    private fun DbToPx(dB: Double): Int {
        return (width*(MIN_DB - dB)/ MIN_DB).roundToInt()
    }

    private fun sampleToDb(value: Short) : Double {
        return 20 * ln(value.toDouble()/Short.MAX_VALUE) / ln(10.0)
    }
}
