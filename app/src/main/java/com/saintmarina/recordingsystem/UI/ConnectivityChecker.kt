package com.saintmarina.recordingsystem.UI

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

const val INTERNET_CHANGED: String = "com.saintmarina.recordingsystem.INTERNET_CHANGED"
private const val TAG = "ConnectivityChecker"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ConnectivityChecker(val context: Context) : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        Log.i(TAG, "Internet AVAILABLE")
        super.onAvailable(network)
        context.sendBroadcast(
            Intent().apply {
                action = INTERNET_CHANGED
                putExtra("InternetState", 1)
            }
        )
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Log.i(TAG, "Internet LOST")
        context.sendBroadcast(
            Intent().apply {
                action = INTERNET_CHANGED
                putExtra("InternetState", 0)
            }
        )
    }
}
