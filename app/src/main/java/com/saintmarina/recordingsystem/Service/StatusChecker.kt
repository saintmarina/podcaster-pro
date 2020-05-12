package com.saintmarina.recordingsystem.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class StatusChecker(): BroadcastReceiver() {
    var power: Boolean = true
    var mic: Boolean = true
    var internet: Boolean = true

    var onChange: ((StatusChecker) -> Unit)? = null

    fun startMonitoring(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)

        }
        context.registerReceiver(this, filter)
    }

    fun stopMonitoring(context: Context) {
        context.unregisterReceiver(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { internet = isInternetWorking(it) }
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
        }
        onChange?.invoke(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInternetWorking(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }
}