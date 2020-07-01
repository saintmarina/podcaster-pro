package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout

class FadeAnimation(context: Context, attributeSet: AttributeSet): RelativeLayout(context, attributeSet) {
    fun hide() {
        visibility = View.INVISIBLE
    }

    fun show() {
        visibility = View.VISIBLE
    }
}