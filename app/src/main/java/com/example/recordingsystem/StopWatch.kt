package com.example.recordingsystem

import android.net.wifi.WifiManager
import android.os.SystemClock

class StopWatch {
    private var runningSince: Long? = null
    private var offset: Long = 0

    fun getElapsedTimeNanos(): Long {
        if (runningSince == null)
            return offset

        return offset + SystemClock.elapsedRealtimeNanos() - runningSince!!
    }

    fun reset() {
        runningSince = null
        offset = 0
    }

    fun start() {
        runningSince = SystemClock.elapsedRealtimeNanos()
    }

    fun stop() {
        offset = getElapsedTimeNanos()
        runningSince = null
    }
}
