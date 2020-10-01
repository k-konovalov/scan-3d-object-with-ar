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
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementViewModel(private val app: Application) : AndroidViewModel(app) {

    val toastMessage = MutableLiveData<String>()
    val removeChild = MutableLiveData<AnchorNode>()
    val angleValue = MutableLiveData<Int>()

    private var triangle: Triangle = Triangle()
    private var tabClicked = false

    private var anchorNode1: AnchorNode? = null
    private var anchorNode2: AnchorNode? = null


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
                saveBitmapToDisk(bitmap, filename ?: "")
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
    private fun generateFilename(): String? {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            .toString() + File.separator + "Sceneform/" + date + "_screenshot.jpeg"
    }

    /** Add saveBitmapToDisk method
     * saveBitmapToDisk() writes out the bitmap to the file.
     */
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)

        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
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
     * рассчитываем по формуле: угол A = arccos(cos BC), где cos BC =  скалярное произведение векторов на произведение векторов
     *
     * @param triangle Triangle - (положение объекта, начальная точка съемки, текущее положение камеры)
     * @return Int - значение угла в градусах
     */
    private fun calculateABAngle(): Int {
        val angleCos =
            (triangle.previousCameraVector.x * triangle.currentCameraVector.x + triangle.previousCameraVector.y * triangle.currentCameraVector.y + triangle.previousCameraVector.z * triangle.currentCameraVector.z) /
                    (sqrt(
                        triangle.previousCameraVector.x.pow(2) + triangle.previousCameraVector.y.pow(
                            2
                        ) + triangle.previousCameraVector.z.pow(2)
                    ) * sqrt(
                        triangle.currentCameraVector.x.pow(2) + triangle.currentCameraVector.y.pow(
                            2
                        ) + triangle.currentCameraVector.z.pow(2)
                    ))

        val angle =
            acos(angleCos) * 180 / Math.PI // умножаем на 180 / Math.PIб чтобы перевсти радианы в градусы
        return angle.toInt()
    }

    /**
     * Меняет начальную точки съемки и текущее положение камеры.
     * A также отображает значение угла
     */
    fun updateAngle(arFragment: MyArFragment, arrowRedDownRenderable: Renderable) {

        val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return
        triangle.currentCameraVector.x = cameraPose.tx()

        val angle = calculateABAngle()

        if (angle == 90) {
            anchorNode2?.let {
                removeChild.value = it
            }

            val anchor2 = arFragment.arSceneView.session?.createAnchor(cameraPose)

            anchorNode2 = AnchorNode(anchor2).apply {
                renderable = arrowRedDownRenderable
                setParent(arFragment.arSceneView?.scene)
            }

            anchorNode2?.worldPosition?.x = cameraPose.tx()

            triangle.previousCameraVector.x = anchorNode2?.worldPosition?.x ?: 0f
        }

        angleValue.postValue(angle)
    }

    fun onTap(hitResult: HitResult, arrowRedDownRenderable: Renderable, arFragment: MyArFragment) {
        if (!tabClicked) {

            tabClicked = true

            createThreeDots(hitResult, arrowRedDownRenderable, arFragment)

            triangle = Triangle(
                Vector(
                    anchorNode1?.worldPosition?.x ?: 0f,
                    anchorNode1?.worldPosition?.y ?: 0f,
                    anchorNode1?.worldPosition?.z ?: 0f
                ),
                Vector(
                    anchorNode2?.worldPosition?.x ?: 0f,
                    anchorNode2?.worldPosition?.y ?: 0f,
                    anchorNode2?.worldPosition?.z ?: 0f
                ),
                Vector(
                    anchorNode2?.worldPosition?.x ?: 0f,
                    anchorNode2?.worldPosition?.y ?: 0f,
                    anchorNode2?.worldPosition?.z ?: 0f
                )
            )

            val currentAngle = calculateABAngle()
            angleValue.postValue(currentAngle)
        }
    }


    private fun createThreeDots(hitResult: HitResult, arrowRedDownRenderable: Renderable, arFragment: MyArFragment) {
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
}