package com.saintmarina.recordingsystem.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi


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
        }

        context.registerReceiver(this, filter)
    }

    fun stopMonitoring(context: Context) {
        context.unregisterReceiver(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        // TODO log the intent we are receiving, intent.action, intent.extra
        Log.i(TAG, "intent.toString() = ${intent.toString()}")
        Log.i(TAG, "intent action = ${intent?.action.toString()} ")
        for (key in intent?.extras?.keySet()!!) {
            val keyValue = if (intent?.extras?.get(key) != null) intent?.extras?.get(key)
                           else "null"
            Log.i(TAG, "key($key): $keyValue")
        }
        Log.i(TAG, "intent?.extras.toString() = ${intent?.extras.toString()}")
        // TODO find intent of changing the internet connectivity
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
        onChange?.invoke()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInternetWorking(context: Context): Boolean {
        return with(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) {
            activeNetwork?.let {
                getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        } ?: false
    }
}