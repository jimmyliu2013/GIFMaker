package com.lim.gifmaker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log


class MainService : Service() {
    private val TAG = "MainService"

    override fun onBind(i: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val overlayWindowController = OverlayWindowController(applicationContext, intent)
        } else {
            Log.e(TAG, "intent for media projection is null!")
            System.exit(1)
        }
        return START_NOT_STICKY
    }
}