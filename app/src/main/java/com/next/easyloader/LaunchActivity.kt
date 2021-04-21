package com.next.easyloader

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        request()
    }

    private fun request() {
        val checkSelfPermission =
            ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")

        if (checkSelfPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.READ_EXTERNAL_STORAGE"), 0xff)
        } else {
            MainActivity.start(this)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

     if (requestCode != 0xff)
         return

        if (grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            MainActivity.start(this)
            finish()
        }
    }
}