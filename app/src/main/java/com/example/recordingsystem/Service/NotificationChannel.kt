package com.example.recordingsystem.Service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.example.recordingsystem.R


const val CHANNEL_ID: String = "1001"

class NotificationChannel: Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sound: Uri =
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.silence)

            val att = AudioAttributes.Builder().apply {
                setUsage(AudioAttributes.USAGE_NOTIFICATION)
                setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording System Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(sound, att.build())
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}