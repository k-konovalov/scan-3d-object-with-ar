package com.arvrlab.reconstructcamera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CustomCameraX {
    private val TAG = "CustomCameraX"
    //Internal Camera
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private val analysisExecutor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var mainExecutor: Executor
    private var cameraProvider: ProcessCameraProvider? = null
    val errorMessage = MutableLiveData<String>("")

    //Internal Camera Settings
    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val RATIO_4_3_VALUE = 4.0 / 3.0
    private val RATIO_16_9_VALUE = 16.0 / 9.0

    //
    val wb = MutableLiveData<Int>(0)
    val focus = MutableLiveData<Int>(0)
    val maxFocus = MutableLiveData<Int>(0)


    fun initCamera(viewLifecycleOwner: LifecycleOwner, internalCameraView: PreviewView, context: Context) {
        mainExecutor = ContextCompat.getMainExecutor(context)
        // Get screen metrics used to setup camera for full screen resolution

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEach {id ->
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            cameraCharacteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.apply { if (isEmpty()) return }
                ?.forEach { manualFocus ->
                    if (manualFocus == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) Log.e(TAG, "Manual AF available on camera id: $id!")
            }
            cameraCharacteristics
                .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)?.also {
                    Log.e(TAG, "Max focus on camera id $id $it")
                    if(maxFocus.value!! < it.toInt()) maxFocus.postValue(it.toInt())
                }
            //if (facing == lensFacing) return@forEach
        }

        val metrics = DisplayMetrics().also { internalCameraView.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = internalCameraView.display.rotation

        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        // Bind the CameraProvider to the LifeCycleOwner
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            futureListener(
                cameraProviderFuture,
                screenAspectRatio,
                rotation,
                cameraSelector,
                internalCameraView,
                viewLifecycleOwner
            ), mainExecutor
        )
    }

    private fun futureListener(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        screenAspectRatio: Int,
        rotation: Int,
        cameraSelector: CameraSelector,
        internalCameraView: PreviewView,
        viewLifecycleOwner: LifecycleOwner
    ) = Runnable {
        val customSize = Size(1920,1080)

        cameraProvider = cameraProviderFuture.get()
        cameraProvider?.unbindAll() // Must unbind the use-cases before rebinding them.

        preview = setupAndBuildPreview(customSize, rotation, screenAspectRatio)
        imageCapture = setupAndBuildImageCapture(screenAspectRatio, rotation)

        bindCamera(viewLifecycleOwner, internalCameraView, cameraSelector)
    }

    private fun setupAndBuildPreview(customSize: Size, rotation: Int, screenAspectRatio: Int): Preview = Preview.Builder().let {
        it.setTargetAspectRatio(screenAspectRatio)
        //it.setTargetResolution(customSize)
        it.setTargetRotation(rotation)

        Camera2Interop.Extender(it).apply {
            //setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            //setCaptureRequestOption(CaptureRequest.AWB, CameraMetadata.CONTROL_AWB_MODE_OFF)
            //.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 30L)//FRAME_DURATION_NS)
            setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 0L)//EXPOSURE_TIME_LIMIT_NS)
            // adjust color correction using seekbar's params
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
            setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(wb.value!!))
            // abjust focus using seekbar's params
            setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focus.value!!.toFloat())
        }

        it.build()
    }

    private fun setupAndBuildImageCapture(screenAspectRatio: Int, rotation: Int) = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

    private fun bindCamera(
        viewLifecycleOwner: LifecycleOwner,
        internalCameraView: PreviewView,
        cameraSelector: CameraSelector
    ) {
        try {
            // A variable number of use-cases can be passed here - camera provides access to CameraControl & CameraInfo
            camera = cameraProvider?.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, preview//, imageCapture
            )

            val captureSize = imageCapture?.attachedSurfaceResolution ?: Size(0, 0)
            val previewSize = preview?.attachedSurfaceResolution ?: Size(0, 0)
            val analyzeSize = imageAnalyzer?.attachedSurfaceResolution ?: Size(0, 0)

            Log.e(TAG, "Use case res: capture_$captureSize preview_$previewSize analyze_$analyzeSize")
            internalCameraView.preferredImplementationMode = PreviewView.ImplementationMode.TEXTURE_VIEW
            preview?.setSurfaceProvider(internalCameraView.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    fun colorTemperature(wbFactor: Int): RggbChannelVector { //0..100
        return RggbChannelVector(
            0.635f + 0.0208333f * wbFactor,
            1.0f,
            1.0f,
            3.7420394f + -0.0287829f * wbFactor
        )
    }
}