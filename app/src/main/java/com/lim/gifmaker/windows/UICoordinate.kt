package com.lim.gifmaker.windows

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.DimenRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lim.gifmaker.MainActivity
import com.lim.gifmaker.R


class UICoordinate(val mContext: Context, windowManager: WindowManager) {
    private val TAG: String = "UICoordinate"
    val DISPLAY_WIDTH: Int
    val DISPLAY_HEIGHT: Int
    val STATUS_BAR_HEIGHT: Int
    val NAVIGATION_BAR_HEIGHT: Int
    var x_frame: Int
    var y_frame: Int
    var width_frame: Int
    var height_frame: Int

    val BUTTON_SIZE: Int

    var x_move: Int
    var y_move: Int

    var x_scale: Int
    var y_scale: Int

    var systemBarsVisible = true

    init {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        DISPLAY_WIDTH = metrics.widthPixels
        DISPLAY_HEIGHT = metrics.heightPixels
        STATUS_BAR_HEIGHT = getBarHeight(mContext, "status_bar_height")
        NAVIGATION_BAR_HEIGHT = getBarHeight(mContext, "navigation_bar_height")
        Log.i(
            TAG, "DISPLAY_WIDTH: " + DISPLAY_WIDTH
                    + ", DISPLAY_HEIGHT: " + DISPLAY_HEIGHT
                    + ", STATUS_BAR_HEIGHT: " + STATUS_BAR_HEIGHT
                    + ", NAVIGATION_BAR_HEIGHT: " + NAVIGATION_BAR_HEIGHT
        )
        x_frame = getResDimen(R.dimen.default_x_frame)
        y_frame = getResDimen(R.dimen.default_y_frame)
        width_frame = Resources.getSystem().getDisplayMetrics().widthPixels;
        val portraitHeight = DISPLAY_WIDTH * width_frame / DISPLAY_HEIGHT
        if (portraitHeight > DISPLAY_HEIGHT) {
            height_frame = getResDimen(R.dimen.default_height_frame)
        } else {
            height_frame = portraitHeight
        }
        BUTTON_SIZE = getResDimen(R.dimen.button_size)
        x_move = x_frame
        y_move = y_frame

        x_scale = x_frame + width_frame - BUTTON_SIZE
        y_scale = y_frame + height_frame - BUTTON_SIZE
        LocalBroadcastManager.getInstance(mContext).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val y_on_screen = intent.getIntExtra("y", -1)
                systemBarsVisible = (y_on_screen != y_frame)
            }
        }, IntentFilter(MainActivity.SYSTEMUI_VISIBILITY_ACTION))
    }

    private fun getResDimen(@DimenRes id: Int): Int {
        return mContext.resources.getDimension(id).toInt()
    }

    private fun getBarHeight(context: Context, name: String): Int {
        var result = 0
        val resourceId: Int =
            context.getResources().getIdentifier(name, "dimen", "android")
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getCurrentFrameCoordinate(): IntArray {
        var result = intArrayOf(x_frame, y_frame, width_frame, height_frame)
        return result
    }

    fun onXMove(x: Int, y: Int) {
        if (x + width_frame <= DISPLAY_WIDTH) {
            x_frame = x
            x_move = x
            x_scale = x + width_frame - BUTTON_SIZE
        }
    }

    fun onYMove(x: Int, y: Int) {
        if (y + height_frame <= DISPLAY_HEIGHT - STATUS_BAR_HEIGHT) {
            y_frame = y
            y_move = y
            y_scale = y + height_frame - BUTTON_SIZE
        }
    }

    fun onXScale(x: Int, y: Int) {
        if ((x <= DISPLAY_WIDTH - BUTTON_SIZE) && (x > x_move + BUTTON_SIZE)) {
            width_frame -= x_scale - x
            x_scale = x
        }
    }

    fun onYScale(x: Int, y: Int) {
        if ((y <= DISPLAY_HEIGHT - STATUS_BAR_HEIGHT - BUTTON_SIZE) && y > y_move + BUTTON_SIZE) {
            height_frame -= y_scale - y
            y_scale = y
        }
    }
}