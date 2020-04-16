package com.example.recordingsystem

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationChannel: Application() {
    val CHANNEL_ID: String = "1001"
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Example Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

}