package com.example.recordingsystem

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import java.util.*

const val MAX_RECORDING_TIME: Long = 3 * (1000 * 60 * 60) // 3 hours 5000//

class ChronoMeter(c_meter: Chronometer) {
    private var chronometer: Chronometer = c_meter
    private var timeWhenStopped: Long = 0
    private var colorAnimation: ObjectAnimator? = null
    private var timeOut: Timer? = null

    fun startChronometer(task: () -> Unit) {
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        startTimeOut(task)
    }

    fun stopChronometer() {
        chronometer.stop()
        timeWhenStopped = chronometer.base
        stopTimeOut()

        if (colorAnimation != null && colorAnimation!!.isStarted)
            stopFlashAnimation()
    }

    fun pauseChronometer() {
        timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
        chronometer.stop()
        startFlashAnimation()
        stopTimeOut()
    }

    fun resumeChronometer(task: () -> Unit) {
        chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
        chronometer.start()
        stopFlashAnimation()
        startTimeOut(task, getCurTime())
    }

    fun maybeResetToZero(): Boolean {
        if (SystemClock.elapsedRealtime() - timeWhenStopped  > 10000) { //10 seconds, can set this value to be whatever we want
            chronometer.base = SystemClock.elapsedRealtime()
            return true
        }
        return false
    }

    private fun startFlashAnimation() {
        colorAnimation = ObjectAnimator.ofInt(
            chronometer,
            "textColor",
            Color.BLACK,
            Color.TRANSPARENT).apply {
            duration = 600
            setEvaluator(ArgbEvaluator())
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopFlashAnimation() {
        colorAnimation!!.end()
        colorAnimation = null
        chronometer.setTextColor(Color.BLACK)
    }

    private fun getCurTime(): Long {
        return SystemClock.elapsedRealtime() - chronometer.base
    }

    private fun startTimeOut(task: () -> Unit, time:Long = 0) {
        timeOut = Timer()
        timeOut!!.schedule(object : TimerTask() {
            override fun run() {
                task()
            }
        }, MAX_RECORDING_TIME - time)
    }

    private fun stopTimeOut() {
        if (timeOut != null) {
            timeOut!!.cancel();
            timeOut = null;
        }
    }
}
