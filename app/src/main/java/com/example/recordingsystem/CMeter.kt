package com.example.recordingsystem

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer

class CMeter(c_meter: Chronometer) {
    private var timeWhenStopped: Long = 0
    private var chronometer: Chronometer = c_meter
    private var isRecording: Boolean = false
    private var colorAnimation: ObjectAnimator? = null

    private fun startFlashAnimation() {
        colorAnimation = ObjectAnimator.ofInt(chronometer, "textColor", Color.BLACK, Color.TRANSPARENT)
        colorAnimation!!.duration = 600
        colorAnimation!!.setEvaluator(ArgbEvaluator())
        colorAnimation!!.repeatCount = ValueAnimator.INFINITE
        colorAnimation!!.repeatMode = ValueAnimator.REVERSE
        colorAnimation!!.start()
    }

    private fun stopFlashAnimation() {
        colorAnimation!!.end()
        colorAnimation = null
        chronometer.setTextColor(Color.BLACK)
    }

    fun startChronometer() {
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        isRecording = true
    }

    fun stopChronometer() {
        chronometer.stop()
        timeWhenStopped = chronometer.base
        isRecording = false

        if (colorAnimation != null && colorAnimation!!.isStarted) {
            stopFlashAnimation()
            Log.e("Time", "inside stopChronometer, stopAnimation")
        }
    }

    fun pauseChronometer() {
        if (isRecording) {
            timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
            chronometer.stop()
            isRecording = false
            startFlashAnimation()
        }
    }

    fun resumeChronometer() {
        if (!isRecording) {
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
            chronometer.start()
            isRecording = true
            stopFlashAnimation()
        }
    }

    fun maybeResetToZero(): Boolean {
        if (SystemClock.elapsedRealtime() - timeWhenStopped  > 10000) { //5 seconds, can set this value to be whatever we want
            chronometer.base = SystemClock.elapsedRealtime()
            return true
        }
        return false
    }

    public fun getCurTime(): Long {
        return SystemClock.elapsedRealtime() - chronometer.base
    }
}
