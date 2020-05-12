package com.saintmarina.recordingsystem.UI

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.saintmarina.recordingsystem.R
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "CheckPermissionActivity"
private const val REQUEST_CODE: Int = 0

class CheckPermissionsActivity : AppCompatActivity() {
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "inside onCreate")
        setContentView(R.layout.activity_main)
        acquirePermissions(permissions)

        try_again_btn.setOnClickListener {
            acquirePermissions(permissions)
        }
    }

    private fun acquirePermissions(permissions: Array<String>) :Unit {
        Log.e(TAG, "inside acquirePermissions")
        for (item in permissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
                return
            }
        }
        val intent = Intent(this, RecordingSystemActivity::class.java)
        startActivity(intent);
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.e(TAG, "inside onRequestPermissionResult")
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    permission_denied.setVisibility(View.VISIBLE)
                    Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show()
                    Log.i("Status", "Permission has been denied by user")
                } else {
                    val intent = Intent(this, RecordingSystemActivity::class.java)
                    startActivity(intent);
                    Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show()
                    Log.i("Status", "Permission has been granted by user")
                }
            }
        }
    }


}
