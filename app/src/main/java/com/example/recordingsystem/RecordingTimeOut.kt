package com.example.recordingsystem

import java.util.*

const val MAX_RECORDING_TIME: Long = 5000//3 * (1000 * 60 * 60) // 3 hours

class RecordingTimeOut {
    var timer: Timer? = null

     fun startTimer(task: () -> Unit, time:Long = 0) {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                task()
            }
        }, MAX_RECORDING_TIME - time)
    }

     fun stopTimer() {
        if (timer != null) {
            timer!!.cancel();
            timer = null;
        }
    }

}