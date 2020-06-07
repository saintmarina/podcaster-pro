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
    var state: RecordingService.State = RecordingService.State()
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
                state.powerAvailable = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val status: Int =  intent.getIntExtra("state", -1);
                 state.micPlugged = status == 1
            }
            INTERNET_CHANGED -> {
                val status: Int =  intent.getIntExtra("InternetState", 0)
                state.internetAvailable = status == 1
            }
        }
        onChange?.invoke()
    }
}