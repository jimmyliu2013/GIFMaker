package com.lim.gifmaker.windows

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.lim.gifmaker.R

class ScaleWindow(context: Context, windowManager: WindowManager) :
    OverlayWindow(context, windowManager) {

    private val TAG = "ScaleWindow"

    init {
        mView = View(mContext)
        mView.setBackgroundResource(R.drawable.ic_expand_outline)
        mView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return onScaleButtonTouch(v, event)
            }
        })
    }

    private fun onScaleButtonTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val nowX = event.rawX.toInt()
                val nowY = event.rawY.toInt()
                mUIChangeListener.onScale(nowX, nowY)
            }
        }
        return true
    }

    override fun initLayoutParamsAndShow(x: Int, y: Int, width: Int, height: Int) {
        mLayoutParams.gravity = Gravity.LEFT or Gravity.TOP
        mLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        super.initLayoutParamsAndShow(x, y, width, height)
    }

}