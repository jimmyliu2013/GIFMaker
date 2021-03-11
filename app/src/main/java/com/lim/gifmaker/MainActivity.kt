package com.lim.gifmaker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast


class MainActivity : Activity() {
    private val TAG = "MainActivity"
    val REQUEST_CODE_OVERLAY = 1
    val REQUEST_CODE_PROJECTION = 2
    lateinit var mHandler: Handler

    companion object {
        val SYSTEMUI_VISIBILITY_ACTION = "action.systemui.visibility"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.LEFT or Gravity.TOP)
        val layoutParams: WindowManager.LayoutParams = window.getAttributes()
        layoutParams.x = 0
        layoutParams.y = 0
        layoutParams.width = 1
        layoutParams.height = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        window.setAttributes(layoutParams)
        mHandler = Handler(Looper.getMainLooper())

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        applicationContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Toast.makeText(
                    applicationContext,
                    R.string.no_orientation_change,
                    Toast.LENGTH_SHORT
                )
                    .show()
                mHandler.postDelayed(Runnable { System.exit(1) }, 2000)

            }

        }, filter)

        requestOverlayPermission()
    }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ), REQUEST_CODE_OVERLAY
            )
        } else {
            requestProjectionPermission()
        }
    }

    fun requestProjectionPermission() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_CODE_PROJECTION
        );
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.no_overlay_permission, Toast.LENGTH_SHORT)
                    .show()
                mHandler.postDelayed(Runnable { System.exit(1) }, 2000)
            } else {
                requestProjectionPermission()
            }

        } else if (requestCode == REQUEST_CODE_PROJECTION) {
            if (data == null || resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    this,
                    R.string.no_projection_permission,
                    Toast.LENGTH_SHORT
                )
                    .show()
                mHandler.postDelayed(Runnable { System.exit(1) }, 2000)
            } else {
                data.setClass(this@MainActivity, MainService::class.java)
                startService(data)
            }
        }
    }
}
