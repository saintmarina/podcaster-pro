package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.activity_recording_system.view.*

class StatusIndicator(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private val painterGreen = Paint().apply {
        color = Color.parseColor("#32CD32")
        isAntiAlias = true }
    private val painterRed = Paint().apply {
        color = Color.parseColor("#FF0000")
        isAntiAlias = true }

    var power: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var internet: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val x = width/2.toFloat()
            val y = height/2.toFloat()
            val radius = 15.toFloat()
            val paint: Paint

            when {
                !power -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Power is not connected."
                }
                !internet -> {
                    paint = painterRed
                    rootView.statusTextView.text = "Internet is not connected."
                }
                else -> {
                    paint = painterGreen
                    rootView.statusTextView.text = "All good."
                }
            }
            it.drawCircle(x, y, radius, paint);
        }
    }
}