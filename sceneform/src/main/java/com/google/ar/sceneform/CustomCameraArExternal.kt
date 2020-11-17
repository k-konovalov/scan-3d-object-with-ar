package com.google.ar.sceneform

import android.content.Context
import android.util.Log
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.UVCCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.webrtc.*
import java.util.concurrent.Executors

private const val TAG = "CustomCameraArExternal"


class CustomCameraArExternal
@JvmOverloads constructor(
    private val camera: UVCCamera = UVCCamera(),
    private var width: Int = 960,
    private var height: Int = 960,
    scene: Scene
) : Camera(scene), VideoCapturer {
    //Coroutines
    private var job = Job()
    private val defScope = CoroutineScope(Dispatchers.Default + job)

    //USBCamera
    private val executor = Executors.newSingleThreadExecutor()

    //for WebRTC
    private lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private lateinit var capturerObserver: CapturerObserver
    private var videoFrame: VideoFrame? = null
    var start = 0L

    var isExternalCameraRunning = false

    //--------------------implement VideoCapturer
    override fun changeCaptureFormat(i: Int, i1: Int, i2: Int) {
        Log.i(TAG, "changeCaptureFormat right fired!")
    }

    override fun isScreencast(): Boolean {
        return false
    }

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper,
        context: Context,
        capturerObserver: CapturerObserver
    ) {
        this.surfaceTextureHelper = surfaceTextureHelper
        this.capturerObserver = capturerObserver

        Log.i(TAG, "USB init")
    }

    /**
     * Notify if the camera have been started successfully or not.
     * Called on a Java thread owned by VideoCapturer. CapturerThread
     * */
    override fun startCapture(i: Int, i1: Int, i2: Int) {
        Log.i(TAG, "USB start Capture")

        if (!isExternalCameraRunning) executor.execute {
            start = System.nanoTime()

            Log.i(TAG, "USB Camera" + camera.device.toString())
            Log.i(TAG, "USB Camera" + camera.deviceName)

            logSupportedSizes()

            val check =
                camera.supportedSizeList!!
                    .filter { (it.width in (width - 1)..(width + 1)) && (it.height in (height - 1)..(height + 1)) }
                    .also {
                        it.forEach {
                            Log.d(TAG, "${it.width} x ${it.height}")
                        }
                    }
            //Use default if not supported cutom res
            if (check.isEmpty()) {
                width = UVCCamera.DEFAULT_PREVIEW_WIDTH
                height = UVCCamera.DEFAULT_PREVIEW_HEIGHT
            }

            Log.e(TAG, "Selected resolution $width x $height")

            camera.apply {
                autoFocus = false
                setPreviewSize(
                    width,
                    height,
                    UVCCamera.FRAME_FORMAT_MJPEG //Work: MJPEG
                )
                //setPreviewDisplay(svVideoRender.holder)
                setFrameCallback(myFrameCallBack, UVCCamera.PIXEL_FORMAT_NV21)
                startPreview()
            }

            capturerObserver.onCapturerStarted(true)
        }
        isExternalCameraRunning = true
    }

    override fun stopCapture() {
        Log.i(TAG, "USB stop capture")
        camera.stopPreview()
        isExternalCameraRunning = false
        surfaceTextureHelper.handler.removeCallbacksAndMessages(null)
    }

    override fun dispose() {
        Log.i(TAG, "USB disposed")
        surfaceTextureHelper.handler?.removeCallbacksAndMessages(null)
        defScope.cancel()
    }

    //--------------------IFrameCallback are most important thing there
    private val myFrameCallBack: IFrameCallback = IFrameCallback { frame ->
        videoFrame = VideoFrame(
            NV12Buffer(
                width,
                height,
                width,
                height,
                frame
            ) { JniCommon.nativeFreeByteBuffer(frame) }, 0, System.nanoTime() - start
        )
        capturerObserver.onFrameCaptured(videoFrame)
    }

    private fun logSupportedSizes() {
        val sizeList = camera.supportedSizeList //List<Size>
        sizeList?.apply {
            var supportedSizes = ""
            forEach {
                supportedSizes += "${it.width}x${it.height} "
            }
            Log.i(TAG, "Support resolution's: $supportedSizes")
        }
    }
}