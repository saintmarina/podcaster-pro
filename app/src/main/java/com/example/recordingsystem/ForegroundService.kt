package com.example.recordingsystem

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

const val FOREGROUND_ID = 1

class ForegroundService(): Service() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_ID, createNotification())
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification(): Notification {
        val pendingIntent = createContentIntent()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_stat_name)
            setContentTitle("Recording system")
            setContentText("Currently recording, don't stop talking. Click on this notification to go back to the app.")
            priority = PRIORITY_HIGH
            setContentIntent(pendingIntent)
        }
        return notification.build()
    }

    private fun createContentIntent(): PendingIntent {
        val notificationIntent = Intent(applicationContext, RecordingSystemActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}