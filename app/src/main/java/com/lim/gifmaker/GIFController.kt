package com.lim.gifmaker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import com.lim.gifmaker.encoder.AnimatedGifEncoder
import com.lim.gifmaker.windows.UICoordinate
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.lang.reflect.Method
import java.nio.ByteBuffer
import kotlin.concurrent.thread


@SuppressLint("WrongConstant")
class GIFController(
    val context: Context,
    intent: Intent,
    val mUICoordinate: UICoordinate
) {
    private val TAG: String = "GIFController"
    private val DEBUG = true

    private val mEncoder: AnimatedGifEncoder
    private var mImageReader: ImageReader
    private var mVirtualDisplay: VirtualDisplay
    private val mMediaProjection: MediaProjection
    private val mMediaProjectionManager: MediaProjectionManager
    private lateinit var mGIFControllerStateCallBack: GIFController.GIFControllerStateCallBack
    private var FRAME_RATE = 8;
    private val MAX_FRAME_NUMBER = 200 // 20s at most
    private var GIF_QUALITY = 20
    private var SCALE = 2 //virtual size/real size
    private var VIRTUAL_WIDTH = mUICoordinate.DISPLAY_WIDTH / SCALE //16:9 display
    private var VIRTUAL_HEIGHT = mUICoordinate.DISPLAY_HEIGHT / SCALE
    private val VIRTUAL_DPI = 160
    private val BYTES_PER_PIXEL = 4
    private val APP_FILE_FOLDER: String
    private val TMP_FOLDER: String
    private val mFrameArray = IntArray(MAX_FRAME_NUMBER)

    @Volatile
    var mStopped = true

    external fun allocateNativeBuffer(size: Int)
    external fun clearNative()
    external fun setFolderName(folder: String)

    external fun processBuffer(
        bb: ByteBuffer,
        index: Int,
        x_frame: Int,
        y_frame: Int,
        width_frame: Int,
        height_frame: Int,
        old_width: Int
    ): Int

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    init {
        mMediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mMediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
        mImageReader =
            ImageReader.newInstance(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "GIFMaker",
            VIRTUAL_WIDTH, VIRTUAL_HEIGHT, VIRTUAL_DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.getSurface(), null, null
        )

        APP_FILE_FOLDER = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath
        TMP_FOLDER = APP_FILE_FOLDER + "/tmp"
        mEncoder = AnimatedGifEncoder()
        mEncoder.setDelay(1000 / FRAME_RATE)
        mEncoder.setRepeat(0)
        mEncoder.setQuality(GIF_QUALITY)
    }

    private fun createTmpFolder() {
        removeTmpFolder()
        var dir = File(TMP_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private fun removeTmpFolder() {
        var file = File(TMP_FOLDER)
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    fun startRecording(uiCoordinate: UICoordinate) {
        //TODO handle exceptions, now just let it crash
        Log.i(TAG, "start recording")
        mStopped = false
        mGIFControllerStateCallBack.onStartRecording()
        val paramArr = uiCoordinate.getCurrentFrameCoordinate()
        createTmpFolder()
        setFolderName(TMP_FOLDER)
        allocateNativeBuffer(paramArr[2] * paramArr[3] * BYTES_PER_PIXEL / SCALE / SCALE)
        thread {
            for (count in 0..MAX_FRAME_NUMBER - 1) {
                if (mStopped == true || count == MAX_FRAME_NUMBER) {
                    mFrameArray[count] = -1
                    break
                }
                if (DEBUG) {
                    Log.d(TAG, "start frame " + count + " " + System.currentTimeMillis())
                }
                var startTime = System.currentTimeMillis()

                val image: Image? = mImageReader.acquireLatestImage()
                if (image == null) {
                    Log.e(TAG, "image is null.")
                    if (count == 0) {
                        throw IllegalStateException("the first frame is null!!!")
                    }
                    mFrameArray[count] = mFrameArray[count - 1]
                    if (count == MAX_FRAME_NUMBER - 1) {
                        mFrameArray[count] = -1

                    }
                    Thread.sleep(1000L / FRAME_RATE)
                    continue
                }

                val planes: Array<Image.Plane> = image.planes
                val buffer: ByteBuffer = planes[0].getBuffer()

                val width: Int = image.getWidth()
                val height: Int = image.getHeight()
                val pixelStride: Int = planes[0].getPixelStride()
                val rowStride: Int = planes[0].getRowStride()
                val rowPadding = rowStride - pixelStride * width


                processBuffer(
                    buffer,
                    count,
                    paramArr[0] / SCALE,
                    (paramArr[1] + if (mUICoordinate.systemBarsVisible) mUICoordinate.STATUS_BAR_HEIGHT else 0) / SCALE,
                    paramArr[2] / SCALE,
                    paramArr[3] / SCALE,
                    width + rowPadding / pixelStride
                )

                image.close()

                mFrameArray[count] = count
                var endTime = System.currentTimeMillis()

                var diff = 1000L / FRAME_RATE - (endTime - startTime)

                if (diff > 0) {
                    if (DEBUG) {
                        Log.d(TAG, "we are fast, sleep " + diff + "ms here")
                    }
                    Thread.sleep(diff)
                }
                if (DEBUG) {
                    Log.d(TAG, "end frame " + count + " " + System.currentTimeMillis())
                }
            }
            clearNative()
            stop()
            Log.d(TAG, "all frame finished")
            generateGIF(paramArr[2] / SCALE, paramArr[3] / SCALE)
        }

    }

    fun stop() {
        Log.d(TAG, "stop recording")
        mStopped = true;
        mGIFControllerStateCallBack.onStopRecording()
    }

    fun generateGIF(width: Int, height: Int) {
        mGIFControllerStateCallBack.onStartGenerating()
        val bufferSize = width * height * BYTES_PER_PIXEL
        val utc = System.currentTimeMillis()
        val fileName = APP_FILE_FOLDER + "/" + utc + ".gif"
        val fos = FileOutputStream(fileName)
        val buffer = ByteBuffer.allocate(bufferSize);
        mEncoder.start(fos)
        for (i in mFrameArray) {
            //skip the first frame, the buttons are still showing
            if (i == 0) {
                continue
            }
            if (i == -1) {
                Log.d(TAG, "reach -1, end")
                break
            }
            if (DEBUG) {
                Log.d(TAG, "generate GIF, processing file " + i + ".data")
            }

            var file = RandomAccessFile(TMP_FOLDER + "/" + i + ".data", "r")
            var inChannel = file.channel
            if (inChannel.read(buffer) == bufferSize) {
                buffer.rewind()
                var bitmap = generateBitmapFromBuffer(buffer, width, height)
                mEncoder.addFrame(bitmap)
                buffer.clear()
            }
            inChannel.close()
        }
        mEncoder.finish()
        fos.close()
        removeTmpFolder()
        mGIFControllerStateCallBack.onStopGenerating(fileName)
        viewImage(fileName)
    }

    fun generateBitmapFromBuffer(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    fun setGIFControllerStateCallBack(_GIFControllerStateCallBack: GIFControllerStateCallBack) {
        mGIFControllerStateCallBack = _GIFControllerStateCallBack
    }

    fun viewImage(fileName: String) {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val m: Method = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.parse("file://" + fileName), "image/*")

        if (intent.resolveActivityInfo(context.packageManager, 0) != null) {
            context.startActivity(intent)
        }
    }

    interface GIFControllerStateCallBack {
        fun onStartRecording()
        fun onStopRecording()
        fun onStartGenerating()
        fun onStopGenerating(fileName: String)
        fun onException(message: String)
    }
}
