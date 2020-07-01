package com.saintmarina.recordingsystem.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.saintmarina.recordingsystem.R

private const val ANIMATION_DURATION = 300L

class FadeAnimation(context: Context, attributeSet: AttributeSet): RelativeLayout(context, attributeSet) {
    private var instantShow = context.obtainStyledAttributes(attributeSet, R.styleable.FadeAnimation)
        .getBoolean(R.styleable.FadeAnimation_instant_show, false)

    var show: Boolean = visibility == View.VISIBLE
    set(value) {
        if (field != value) {
            field = value
            if (value)
                show()
            else
                hide()
        }
    }

    private fun convertInvisibilityIntoAlpha() {
        if (visibility == View.INVISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
        }
    }

    private fun hide() {
        convertInvisibilityIntoAlpha()

        animate().run {
            cancel()
            duration = ANIMATION_DURATION
            alpha(0f)
            start()
        }
    }

    private fun show() {
        convertInvisibilityIntoAlpha()
        if (instantShow) {
            alpha = 1f
            return
        }

        animate().run {
            cancel()
            duration = ANIMATION_DURATION
            alpha(1f)
            start()
        }
    }
}