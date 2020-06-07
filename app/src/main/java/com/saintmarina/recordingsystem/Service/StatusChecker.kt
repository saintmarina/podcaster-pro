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
class StatusChecker: BroadcastReceiver() {
    var state: RecordingService.State = RecordingService.State()
    var onChange: (() -> Unit)? = null

    private var connectivityChecker = ConnectivityChecker()
    private var connectivityManager: ConnectivityManager? = null

    fun startMonitoring(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        registerNetworkCallback(context)
        context.registerReceiver(this, filter)
    }

    fun stopMonitoring(context: Context) {
        connectivityManager?.unregisterNetworkCallback(connectivityChecker)
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

    private fun registerNetworkCallback(context: Context) {
        Log.i(TAG, "registering the NetworkCallback. Starting to monitor internet status")
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager?.registerNetworkCallback(networkRequest, connectivityChecker)
    }

    inner class ConnectivityChecker : ConnectivityManager.NetworkCallback() {
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

}