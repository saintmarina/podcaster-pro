package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class StatusIndicator(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val painterGreen = Paint().apply { color = Color.parseColor("#32CD32"); isAntiAlias =
        true }
    private val painterOrange = Paint().apply { color = Color.parseColor("#FC6A03"); isAntiAlias =
        true }
    private val painterRed = Paint().apply { color = Color.parseColor("#FF0000"); isAntiAlias =
        true }

    var power: Boolean = false
    var mic: Boolean = false
    var internet: Boolean = false


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            var x = width/2.toFloat()
            var y = height/2.toFloat()
            var radius = 15.toFloat()
            var paint = painterGreen

            if (!power || !internet) {
                paint = painterOrange
            }else if (!mic) {
                paint = painterRed
            }

            it.drawCircle(x, y, radius, paint);
        }
    }
}