package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

const val MIN_DB = -50.0

class SoundVisualizer (context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val painterGreen = Paint().apply { color = Color.parseColor("#32CD32") }
    private val painterYellow = Paint().apply { color = Color.parseColor("#888800") }
    private val painterRed = Paint().apply { color = Color.parseColor("#FF0000") }

    var volume: Short = 0
        set(value) {
            field = value
            invalidate()
        }

    var didClip: Boolean = false


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {

            val maxPx = DbToPx(sampleToDb(volume))

            //Log.i("SoundVisualizer", "maxDb is $maxDb")
            //Log.i("SoundVisualizer", "dB of volume is ${shortIntoDb(volume)}")
            //Log.i("SoundVisualizer", "max is ${max}")
            //val maxMaxVolume = (width*maxVolume)/Short.MAX_VALUE
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

    fun DbToPx(dB: Double): Int {
        return (width*(MIN_DB - dB)/MIN_DB).roundToInt()
    }

    private fun sampleToDb(value: Short) : Double {
        return 20 * ln(value.toDouble()/Short.MAX_VALUE) / ln(10.0)
    }
}
//TODO always indicate the highest volume ever recorded. If the highest volume ever recorded reached 32K - display thick squares in the end of the SoundBar
//TODO convert all the values in db
//db = 10*Math.log(volume/Short.MAX_VALUE)/Math.log(10) //The width is max - -46  - 0
