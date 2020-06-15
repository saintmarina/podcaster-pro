package com.saintmarina.recordingsystem

import java.io.InputStream

private const val SEC_IN_NANO: Long = 1_000_000_000
private const val TAG = "Util"

object Util {
    @JvmStatic fun formatAudioDuration(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / (60 * 60)

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    @JvmStatic fun nanosToSec(time: Long): Int {
        return (time / SEC_IN_NANO).toInt()
    }
    @JvmStatic fun readString(inputStream: InputStream): String {
        return String(inputStream.readBytes())
    }

    @JvmStatic fun prettyDuration(time: Int): String {
        val seconds = time % 60
        val minutes = time / 60 % 60
        val hours = time / (60 * 60)

        if (hours != 0) {
            return if (hours == 1) "$hours hour" else "$hours hours"
        }
        if (minutes != 0) {
            return if (minutes == 1) "$minutes min" else "$minutes mins"
        }

        return "$seconds sec"
    }
}
