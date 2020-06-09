package com.saintmarina.recordingsystem.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

private const val TAG = "StatusChecker"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StatusChecker(val context: Context): BroadcastReceiver() {
    // We can't access context variable until the Service is created
    var state: RecordingService.State = RecordingService.State()
    var onChange: (() -> Unit)? = null

    private var connectivityChecker = object: ConnectivityManager.NetworkCallback()  {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "Internet AVAILABLE")
            super.onAvailable(network)
            state.internetAvailable = true
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i(TAG, "Internet LOST")
            state.internetAvailable = false
        }
    }

    private var connectivityManager: ConnectivityManager? = null

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(this, filter)
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        Log.i(TAG, "registering the NetworkCallback. Starting to monitor internet status")
        // We can only set the connectivityManager variable after "onCreate" of the Service has run
        connectivityManager = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            registerNetworkCallback(networkRequest, connectivityChecker)
        }
    }

    fun stopMonitoring() {
        connectivityManager?.unregisterNetworkCallback(connectivityChecker)
        connectivityManager = null
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "received intent action ${intent?.action.toString()}")
        when (intent?.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                state.powerAvailable = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val status: Int = intent.getIntExtra("state", -1)
                 state.micPlugged = status == 1
            }
        }
        onChange?.invoke()
    }
}