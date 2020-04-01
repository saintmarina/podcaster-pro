package com.example.recordingsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class SoundVisualizer (context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    val painter = Paint()

    var volume: Short = 0
        set(value) {
            field = value
            invalidate()
        }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = 200

        setMeasuredDimension(parentWidth, desiredHeight)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }



    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            val right = (width*volume)/Short.MAX_VALUE
            val rect = Rect(0, 0, right, height)
            it.drawRect(rect, painter)
        }

    }
}
