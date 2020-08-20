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

    //EV & WB
    val wb = MutableLiveData<Int>(0)
    val focus = MutableLiveData<Int>(0)
    val maxFocus = MutableLiveData<Int>()
    val iso = MutableLiveData<Int>()
    val maxIso = MutableLiveData<Int>()
    val exposure = MutableLiveData<Int>()
    val maxExposure = MutableLiveData<Int>()
    val frameDuration = MutableLiveData<Int>()
    val maxFrameDuration = MutableLiveData<Long>()

    fun initCamera(viewLifecycleOwner: LifecycleOwner, internalCameraView: PreviewView, context: Context) {
        mainExecutor = ContextCompat.getMainExecutor(context)
        // Get screen metrics used to setup camera for full screen resolution
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

    fun logAndSetupAvailableCameraSettings(context: Context){
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEach {id ->
            var cameraLog = "Camera $id:"
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

            //Supported HW Level
            cameraCharacteristics
                .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                ?.apply { cameraLog += "\nHardwareLevel Full: ${this == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL}" }
            //AE
            cameraCharacteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.apply { if (isEmpty()) return }
                ?.forEach { capability ->
                    if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) cameraLog += "\nManual AE: available"
                }
            //FOCUS
            cameraCharacteristics
                .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)?.also {
                    cameraLog += "\nMax focus: $it"
                    if(id == "0") maxFocus.postValue(it.toInt())
                }
            //ISO
            cameraCharacteristics
                .get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                ?.run {
                    cameraLog += "\nISO: $lower - $upper"
                    if(id == "0") {
                        iso.postValue(lower)
                        maxIso.postValue(upper)
                    }
                }
            //Exposure
            cameraCharacteristics
                .get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                ?.run {
                    cameraLog += "\nExposure time: ${lower.toMS()}ms - ${upper.toMS()}ms"
                    if(id == "0") {
                        exposure.postValue(lower.toMS())
                        maxExposure.postValue(upper.toMS())}
                }
            //FrameDuration
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
                ?.run {
                    cameraLog += "\nMax Frame Duration time: ${this.toMS()}ms"
                    if(id == "0") {
                        frameDuration.postValue(this.toMS())
                        maxFrameDuration.postValue(this)
                    }
                }

            Log.e(TAG, cameraLog)
            //if (facing == lensFacing) return@forEach
        }
    }

    private fun futureListener(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        screenAspectRatio: Int,
        rotation: Int,
        cameraSelector: CameraSelector,
        internalCameraView: PreviewView,
        viewLifecycleOwner: LifecycleOwner
    ) = Runnable {
        val custoMSize = Size(1920,1080)

        cameraProvider = cameraProviderFuture.get()
        cameraProvider?.unbindAll() // Must unbind the use-cases before rebinding them.

        logCurrentCameraSettings()
        preview = setupAndBuildPreview(custoMSize, rotation, screenAspectRatio)
        imageCapture = setupAndBuildImageCapture(screenAspectRatio, rotation)

        bindCamera(viewLifecycleOwner, internalCameraView, cameraSelector)
    }

    private fun logCurrentCameraSettings(){
        var log = "Current Camera Settings:"
        log += "\nFocus: ${focus.value}"
        log += "\nISO: ${iso.value}"
        log += "\nExposure: ${exposure.value}ms"
        //log += "\nFrame Duration: ${frameDuration.value}ms"
        Log.e (TAG, log)
    }

    private fun setupAndBuildPreview(custoMSize: Size, rotation: Int, screenAspectRatio: Int): Preview = Preview.Builder().let {
        it.setTargetAspectRatio(screenAspectRatio)
        //it.setTargetResolution(custoMSize)
        it.setTargetRotation(rotation)

        Camera2Interop.Extender(it).apply {
            //Turn on Manual control
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
            // adjust WB using seekbar's params
            setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(wb.value!!))
            // abjust FOCUS using seekbar's params
            setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focus.value!!.toFloat())
            // abjust ISO using seekbar's params
            setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso.value!!)
            /** If we disabling auto-exposure, we need to set the exposure time, in addition to the sensitivity.
            You also preferably need to set the frame duration, though the defaults for both are probably 1/30s */
            // abjust Exposure using seekbar's params
            setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 66.toNS()) //MS -> NS
            // abjust Frame Duration using seekbar's params
            setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 66.toNS()) //MS -> NS
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

    private fun colorTemperature(wbFactor: Int): RggbChannelVector { //0..100
        return RggbChannelVector(
            0.635f + 0.0208333f * wbFactor,
            1.0f,
            1.0f,
            3.7420394f + -0.0287829f * wbFactor
        )
    }

    private fun Long.toMS(): Int = (this / 1000L).toInt() // NS -> MS
    private fun Int.toNS(): Long = (this * 1000 * 1000L)
}