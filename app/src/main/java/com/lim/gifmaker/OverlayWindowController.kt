package com.lim.gifmaker

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.lim.gifmaker.windows.*


class OverlayWindowController(var mContext: Context, intent: Intent) :
    OverlayWindow.UIChangeCallBack, GIFController.GIFControllerStateCallBack {
    private val TAG: String = "OverlayWindowController"
    private val DEBUG = true

    private val mFrameWindow: FrameWindow
    private val mMoveWindow: MoveWindow
    private val mScaleWindow: ScaleWindow
    private val mPanelWindow: PanelWindow
    private val mWindowManager: WindowManager
    private val mUICoordinate: UICoordinate
    private val mHandler: Handler
    private var mGIFController: GIFController

    @Volatile
    private var isRecording = false

    @Volatile
    private var isGenerating = false


    init {
        mHandler = Handler(Looper.getMainLooper())
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mUICoordinate = UICoordinate(mContext, mWindowManager)
        mGIFController = GIFController(mContext, intent, mUICoordinate)
        mFrameWindow = FrameWindow(mContext, mWindowManager)
        mFrameWindow.setListener(this)
        mMoveWindow = MoveWindow(mContext, mWindowManager)
        mMoveWindow.setListener(this)
        mScaleWindow = ScaleWindow(mContext, mWindowManager)
        mScaleWindow.setListener(this)

        mPanelWindow = PanelWindow(mContext, mWindowManager)
        mPanelWindow.setListener(this)
        mPanelWindow.mPlayAndStopButton.setOnClickListener { v ->
            if (!isRecording && !isGenerating) {
                mGIFController.startRecording(mUICoordinate)
            } else if (isRecording && !isGenerating) {
                mGIFController.stop()
            }
        }
        mPanelWindow.mSettingsButton.setOnClickListener { v ->
            showSettingsPopup()
        }
        mPanelWindow.mExitButton.setOnClickListener { v ->
            System.exit(0);
        }
        mGIFController.setGIFControllerStateCallBack(this)

        initUI()
    }

    fun showSettingsPopup() {
        val builder = AlertDialog.Builder(mContext, R.style.MyAlertDialogStyle)
        builder.setMessage(R.string.info)
        builder.setCancelable(true)
        builder.setPositiveButton("OK", null)
        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.window!!.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    fun initUI() {
        mFrameWindow.initLayoutParamsAndShow(
            mUICoordinate.x_frame,
            mUICoordinate.y_frame,
            mUICoordinate.width_frame,
            mUICoordinate.height_frame
        )
        mMoveWindow.initLayoutParamsAndShow(
            mUICoordinate.x_move,
            mUICoordinate.y_move,
            mUICoordinate.BUTTON_SIZE,
            mUICoordinate.BUTTON_SIZE
        )
        mScaleWindow.initLayoutParamsAndShow(
            mUICoordinate.x_scale,
            mUICoordinate.y_scale,
            mUICoordinate.BUTTON_SIZE,
            mUICoordinate.BUTTON_SIZE
        )

        mPanelWindow.initLayoutParamsAndShow(
            y = mUICoordinate.NAVIGATION_BAR_HEIGHT
        )
    }


    override fun onMove(x: Int, y: Int) {
        mUICoordinate.onXMove(x, y)
        mUICoordinate.onYMove(x, y)

        mFrameWindow.setPosition(mUICoordinate.x_frame, mUICoordinate.y_frame)
        mMoveWindow.setPosition(mUICoordinate.x_move, mUICoordinate.y_move)
        mScaleWindow.setPosition(mUICoordinate.x_scale, mUICoordinate.y_scale)
    }

    override fun onScale(x: Int, y: Int) {
        mUICoordinate.onXScale(x, y)
        mUICoordinate.onYScale(x, y)

        mFrameWindow.setSize(mUICoordinate.width_frame, mUICoordinate.height_frame)
        mScaleWindow.setPosition(mUICoordinate.x_scale, mUICoordinate.y_scale)
    }

    override fun onPanelMove(y: Int) {
        mPanelWindow.setPanelPosition(mUICoordinate.DISPLAY_HEIGHT - y)
    }

    override fun onStartRecording() {
        // this is called from main thread
        isRecording = true
        hideMoveAndScaleButton()
        mPanelWindow.mPlayAndStopButton.setImageResource(R.drawable.ic_stop_black_18dp)
    }

    private fun hideMoveAndScaleButton() {
        mMoveWindow.setVisibility(View.GONE)
        mScaleWindow.setVisibility(View.GONE)
    }

    override fun onStopRecording() {
        isRecording = false
    }

    override fun onStartGenerating() {
        isGenerating = true
        mHandler.post(Runnable {
            mPanelWindow.mPlayAndStopButton.setImageResource(R.drawable.ic_hourglass_empty_black_18dp)
            mPanelWindow.mPlayAndStopButton.isEnabled = false
        })
    }

    override fun onStopGenerating(fileName: String) {
        isGenerating = false
        mHandler.post(Runnable {
            showMoveAndScaleButton()
            mPanelWindow.mPlayAndStopButton.setImageResource(R.drawable.ic_play_arrow_black_18dp)
            mPanelWindow.mPlayAndStopButton.isEnabled = true
        })
    }

    override fun onException(message: String) {
        TODO("Not yet implemented")
    }

    private fun showMoveAndScaleButton() {
        mMoveWindow.setVisibility(View.VISIBLE)
        mScaleWindow.setVisibility(View.VISIBLE)
    }


}