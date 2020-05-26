package com.saintmarina.recordingsystem

import android.util.Log
import com.google.gson.Gson
import com.saintmarina.recordingsystem.GoogleDrive.GoogleDriveFile
import org.mortbay.util.ajax.JSON
import java.io.File

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
}
