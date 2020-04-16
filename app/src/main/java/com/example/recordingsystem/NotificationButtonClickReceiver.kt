package com.example.recordingsystem

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class NotificationButtonClickReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
       Log.e("Time", "Action received")
    }
}