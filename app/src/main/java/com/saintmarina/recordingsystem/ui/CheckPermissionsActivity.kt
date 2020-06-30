package com.saintmarina.recordingsystem.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.saintmarina.recordingsystem.R
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "CheckPermissionActivity"
private const val REQUEST_CODE: Int = 0

class CheckPermissionsActivity : Activity() {
    @RequiresApi(Build.VERSION_CODES.P)
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.INTERNET
    )

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "inside onCreate CheckPermissionActivity")
        setContentView(R.layout.activity_main)
        acquirePermissions(permissions)

        try_again_btn.setOnClickListener {
            acquirePermissions(permissions)
        }
    }

    override fun onResume() {
        super.onResume()
        startRecordingSystemActivity()
    }

    private fun acquirePermissions(permissions: Array<String>) :Unit {
        Log.i(TAG, "acquirePermissions if not granted")
        for (item in permissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
                return
            }
        }

        startRecordingSystemActivity()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                permission_denied.visibility = View.VISIBLE
                Log.i(TAG, "Permission denied by user")
            } else {
                Log.i(TAG, "Permission granted by user")
                startRecordingSystemActivity()
            }
        }
    }

    private fun startRecordingSystemActivity() {
        startActivity(Intent(this, RecordingSystemActivity::class.java))
    }
}

