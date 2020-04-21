package com.example.recordingsystem

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import androidx.annotation.RequiresApi

const val ONE_MILISEC = 1000

class Timer {
    var time = 0
    var timeWhenStopped:Long = 0
    private lateinit var handler: Handler

    private val runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            time++
            handler.postDelayed(this, 1000)
        }
    }

    fun startTimer() {
        handler = Handler(Looper.getMainLooper())
        handler.post(runnable)
    }

    fun stopTimer() {
        timeWhenStopped = SystemClock.elapsedRealtime()
        handler.removeCallbacks(runnable)
    }

    fun pauseTimer() {
        handler.removeCallbacks(runnable)
    }

    fun resumeTime() {
        handler.post(runnable)
    }

    fun maybeResetToZero(): Boolean {
        if (SystemClock.elapsedRealtime() - timeWhenStopped  > 3000) { //3 seconds, can set this value to be whatever we want
            time = 0
            return true
        }
        return false
    }

    /* private fun startTimeOut(task: () -> Unit, time:Long = 0) {
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
  }*/
}

/*
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
 */