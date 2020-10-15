package com.test.measurement

import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Renderable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementViewModel(private val app: Application) : AndroidViewModel(app) {

    val toastMessage = MutableLiveData<String>()

    val removeChild = MutableLiveData<AnchorNode>()

    val angleValue = MutableLiveData<Int>()

    val distanceAB = MutableLiveData<Float>()
    val distanceAC = MutableLiveData<Float>()

    val currentDistanceFromTheFloor = MutableLiveData<Float>()
    val modelDistanceFromTheFloor = MutableLiveData<Float>()

    private var anchorNode1: AnchorNode? = null
    private var anchorNode2: AnchorNode? = null

    private var triangle: Triangle = Triangle()

    private var tabClicked = false

    /** Add the takePhoto method
     * The method takePhoto() uses the PixelCopy API to capture a screenshot of the ArSceneView.
     * It is asynchronous since it actually happens between frames. When the listener is called,
     * the bitmap is saved to the disk, and then a snackbar is shown with an intent to open the image
     * in the Pictures application.
     */
    fun takePhoto(view: ArSceneView) {
        val filename = generateFilename()

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(
            view.width, view.height,
            Bitmap.Config.ARGB_8888
        )

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                saveBitmapToDisk(bitmap, filename)
                toastMessage.postValue("Photo saved")
            } else {
                toastMessage.postValue("Failed to copyPixels: $copyResult")
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    /** Add generateFilename method
     * A unique file name is needed for each picture we take.
     * The filename for the picture is generated using the standard pictures
     * directory, and then an album name of Sceneform. Each image name is based
     * on the time, so they won't overwrite each other. This path is also related
     * to the paths.xml file we added previously.
     */
    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            .toString() + File.separator + "Sceneform/" + date + "_screenshot.jpeg"
    }

    /** Add saveBitmapToDisk method
     * saveBitmapToDisk() writes out the bitmap to the file.
     */
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)

        out.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (e: IOException) {
            toastMessage.postValue(e.toString())
        }
    }

    /**
     * Меняет начальную точки съемки и текущее положение камеры.
     * A также отображает значение угла
     */
    fun updateAngle(arFragment: MyArFragment, arrowRedDownRenderable: Renderable) {

        val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return
        triangle.currentCameraVector.x = cameraPose.tx()
        triangle.currentCameraVector.y = cameraPose.ty()
        triangle.currentCameraVector.z = cameraPose.tz()

        val angle = triangle.calculateABAngle()

        if (angle == 90 && (triangle.currentCameraVector.x >= triangle.previousCameraVector.x - 10 && triangle.currentCameraVector.x <= triangle.previousCameraVector.x + 10)
            && (triangle.currentCameraVector.y >= triangle.previousCameraVector.y - 10 && triangle.currentCameraVector.y <= triangle.previousCameraVector.y + 10)) {
            anchorNode2?.let {
                removeChild.value = it
            }

            val anchor2 = arFragment.arSceneView.session?.createAnchor(cameraPose)

            anchorNode2 = AnchorNode(anchor2).apply {
                renderable = arrowRedDownRenderable
                setParent(arFragment.arSceneView?.scene)
            }

            anchorNode2?.worldPosition?.let { newCoords ->

                triangle.previousCameraVector.apply {
                    x = anchorNode2?.worldPosition?.x ?: 0f
                    y = anchorNode2?.worldPosition?.y ?: 0f
                    z = anchorNode2?.worldPosition?.z ?: 0f
                }
            }
        }
        angleValue.postValue(angle)
    }

    fun onTap(hitResult: HitResult, arrowRedDownRenderable: Renderable, arFragment: MyArFragment) {
        if (!tabClicked) {

            val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return

            tabClicked = true

            createThreeDots(hitResult, arrowRedDownRenderable, arFragment)

            val vector1 = anchorNode1?.worldPosition?.let { Vector(it.x, it.y, it.z) } ?: return
            val vector2 = Vector(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())

            triangle = Triangle(vector1,vector2, vector2.copy())

            angleValue.postValue(triangle.calculateABAngle())
        }
    }

    private fun createThreeDots(
        hitResult: HitResult,
        arrowRedDownRenderable: Renderable,
        arFragment: MyArFragment
    ) {
        val anchor = hitResult.createAnchor()

        anchorNode1 = AnchorNode(anchor).apply {
            renderable = arrowRedDownRenderable
            setParent(arFragment.arSceneView?.scene)
        }

        val pose = arFragment.arSceneView.arFrame?.camera?.pose
        val anchor2 = arFragment.arSceneView.session?.createAnchor(pose)

        anchorNode2 = AnchorNode(anchor2).apply {
            renderable = arrowRedDownRenderable
            setParent(arFragment.arSceneView?.scene)
        }

    }

    fun measureAngleFromTheFloor(arFragment: MyArFragment) {
        val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return

        val currentHeight = (cameraPose.ty() + 1.0f) * 100
        val modelHeight = (triangle.previousCameraVector.y + 1.0f) * 100
        currentDistanceFromTheFloor.postValue(currentHeight)
        modelDistanceFromTheFloor.postValue(modelHeight)
    }

    fun showDistances() {
        val distAB = calculateVectorDistance(triangle.previousCameraVector, triangle.objectVector)
        distanceAB.postValue(distAB)
    }

    private fun calculateVectorDistance(vector1: Vector, vector2: Vector): Float {
        val x = vector1.x - vector2.x
        val y = vector1.y - vector2.y
        val z = vector1.z - vector2.z
        return calculateDistance(x, y, z)
    }

    private fun calculateDistance(x: Float, y: Float, z: Float): Float =
        sqrt(x.pow(2) + y.pow(2) + z.pow(2))


    fun measureDistanceFromCamera(arFragment: MyArFragment) {
        val frame = arFragment.arSceneView?.arFrame
        distanceAC.postValue(
            calculateDistance(
                anchorNode1?.worldPosition ?: return,
                frame?.camera?.pose ?: return
            )
        )
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Pose): Float {
        return calculateDistance(
            objectPose0.x - objectPose1.tx(),
            objectPose0.y - objectPose1.ty(),
            objectPose0.z - objectPose1.tz()
        )
    }

}