package com.saintmarina.recordingsystem.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.saintmarina.recordingsystem.UI.INTERNET_CHANGED

private const val TAG = "StatusChecker"

class StatusChecker(): BroadcastReceiver() {
    var power: Boolean = true
    var mic: Boolean = true
    var internet: Boolean = true
    var onChange: (() -> Unit)? = null

    fun startMonitoring(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(INTERNET_CHANGED)
        }
        context.registerReceiver(this, filter)
    }

    fun stopMonitoring(context: Context) {
        context.unregisterReceiver(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "received intent action ${intent?.action.toString()}")
        when (intent?.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                power = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state: Int =  intent.getIntExtra("state", -1);
                mic = state == 1
            }
            INTERNET_CHANGED -> {
                val state: Int =  intent.getIntExtra("InternetState", 0)
                internet = state == 1
            }
        }
        onChange?.invoke()
    }
}