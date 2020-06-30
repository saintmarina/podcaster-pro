package com.saintmarina.recordingsystem

import android.os.Handler
import android.os.Looper


open class OneshotTimer(private val delayMillis: Long,
                        private val callback: (OneshotTimer) -> Unit): Runnable {
    private val handler = Handler(Looper.getMainLooper())

    init {
        schedule()
    }

    override fun run() {
        callback(this)
    }

    fun schedule() {
        handler.postDelayed(this, delayMillis)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }

    fun reset() {
        stop()
        schedule()
    }
}

class RepeatTimer(delayMillis: Long, callback: () -> Unit):
    OneshotTimer(delayMillis, {
        callback()
        it.schedule()
    })