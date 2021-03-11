package com.lim.gifmaker.windows

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lim.gifmaker.MainActivity
import com.lim.gifmaker.R

class FrameWindow(context: Context, windowManager: WindowManager) :
    OverlayWindow(context, windowManager) {

    init {
        mView = LayoutInflater.from(mContext).inflate(R.layout.frame_layout, null)
        mView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            var arr = IntArray(2)
            mView.getLocationOnScreen(arr)
            val intent = Intent(MainActivity.SYSTEMUI_VISIBILITY_ACTION)
            intent.putExtra("y", arr[1])
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)

        }
    }

    override fun initLayoutParamsAndShow(x: Int, y: Int, width: Int, height: Int) {
        mLayoutParams.gravity = Gravity.LEFT or Gravity.TOP
        mLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        super.initLayoutParamsAndShow(x, y, width, height)
    }
}