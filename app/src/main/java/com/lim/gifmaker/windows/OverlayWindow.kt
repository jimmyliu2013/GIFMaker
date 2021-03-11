package com.lim.gifmaker.windows

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager

abstract class OverlayWindow(context: Context, windowManager: WindowManager) {
    protected var mContext: Context
    protected var mLayoutParams = WindowManager.LayoutParams()
    protected var mWindowManager: WindowManager
    protected lateinit var mView: View
    protected lateinit var mUIChangeListener: OverlayWindow.UIChangeCallBack

    init {
        mContext = context
        mWindowManager = windowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        mLayoutParams.format = PixelFormat.RGBA_8888
    }

    open fun initLayoutParamsAndShow(x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) {
        mLayoutParams.width = width
        mLayoutParams.height = height
        mLayoutParams.x = x
        mLayoutParams.y = y
        mWindowManager.addView(mView, mLayoutParams)
    }

    fun setListener(listener: UIChangeCallBack) {
        mUIChangeListener = listener
    }

    open fun setPosition(x: Int, y: Int) {
        mLayoutParams.x = x
        mLayoutParams.y = y
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }

    open fun setSize(width: Int, height: Int) {
        mLayoutParams.width = width
        mLayoutParams.height = height
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }

    fun setPanelPosition(y: Int) {
        mLayoutParams.y = y
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }

    fun setVisibility(visibility: Int) {
        mView.visibility = visibility
    }

    interface UIChangeCallBack {
        fun onMove(x: Int, y: Int)
        fun onScale(x: Int, y: Int)
        fun onPanelMove(y: Int)
    }
}