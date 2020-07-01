package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import com.saintmarina.recordingsystem.R
import kotlin.math.ln
import kotlin.math.roundToInt

const val MIN_DB = -50.0f
const val NUM_RECTS = 56
const val RED_DB_THRESHOLD = -8.0f
const val RECT_OFFSET_X_PX = 23f

class SoundVisualizer (context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val whiteRect = BitmapFactory.decodeResource(context.resources, R.drawable.volume_bar_white, BitmapFactory.Options().apply { inScaled = false })
    private val redRect   = BitmapFactory.decodeResource(context.resources, R.drawable.volume_bar_red, BitmapFactory.Options().apply { inScaled = false })
    private val redRectsThresholdIndex = rectIndex(RED_DB_THRESHOLD)

    var volume: Float = MIN_DB-1f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val numRects = rectIndex(volume)
            for (i in 0..numRects) {
                val rect = if (i < redRectsThresholdIndex) whiteRect else redRect
                it.drawBitmap(rect, i * RECT_OFFSET_X_PX, 0f, null)
            }
        }
    }

    private fun rectIndex(dB: Float): Int {
        return (NUM_RECTS * (MIN_DB - dB) / MIN_DB).roundToInt()
    }

    fun sampleToDb(value: Float) : Float {
        return (20 * ln(value.toDouble()) / ln(10.0)).toFloat()
    }
}
