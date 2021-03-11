package com.lim.gifmaker.windows

import android.content.Context
import android.view.*
import android.widget.ImageButton
import com.lim.gifmaker.R


class PanelWindow(context: Context, windowManager: WindowManager) :
    OverlayWindow(context, windowManager) {

    val mExitButton: ImageButton
    val mPlayAndStopButton: ImageButton
    val mSettingsButton: ImageButton

    init {
        mView = LayoutInflater.from(mContext).inflate(R.layout.panel_layout, null)
        mPlayAndStopButton = mView.findViewById(R.id.btn_play_and_stop)
        mSettingsButton = mView.findViewById(R.id.btn_settings)
        mExitButton = mView.findViewById(R.id.btn_exit)
        mView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return onPanelWindowTouch(v, event)
            }
        })

    }

    private fun onPanelWindowTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val nowY = event.rawY.toInt()
                mUIChangeListener.onPanelMove(nowY)
            }
        }
        return true
    }

    override fun initLayoutParamsAndShow(x: Int, y: Int, width: Int, height: Int) {
        mLayoutParams.gravity = Gravity.RIGHT or Gravity.BOTTOM
        mLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        mLayoutParams.y = y
        mWindowManager.addView(mView, mLayoutParams)
    }
}