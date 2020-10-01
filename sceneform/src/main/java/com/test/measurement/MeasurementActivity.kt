package com.test.measurement

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.PixelCopy
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_measurement.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementActivity : AppCompatActivity(R.layout.activity_measurement) {

    private val arFragment: ArFragment by lazy { supportFragmentManager.findFragmentById(R.id.fragmentSceneform) as ArFragment }

    private var arrowRedDownRenderable: Renderable? = null

    private var triangle: Triangle? = null

    private var anchorNode1: AnchorNode? = null

    private var anchorNode2: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkIsSupportedDeviceOrFinish()
        initArrow()
        initOnTabListener()

        arFragment.arSceneView?.scene?.addOnUpdateListener {
            updateAngle()
        }

        btnTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun checkIsSupportedDeviceOrFinish() {
        val openGlVersionString =
            (Objects.requireNonNull(getSystemService(Context.ACTIVITY_SERVICE)) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion

        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Toast.makeText(
                this,
                "Sceneform requires OpenGL ES $MIN_OPENGL_VERSION or later",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    /**
     * инициальзирует объект Renderable для отображения на сцене
     */
    private fun initArrow() {
        val arrowRedDownLinearLayout = LinearLayout(this)
        val arrowRedDownView = ImageView(this).also {
            it.setImageResource(R.drawable.arrow)
        }

        arrowRedDownLinearLayout.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
            .addView(
                arrowRedDownView,
                arrowViewSize,
                arrowViewSize
            )

        ViewRenderable
            .builder()
            .setView(this, arrowRedDownLinearLayout)
            .build()
            .thenAccept { renderable ->
                arrowRedDownRenderable = renderable
                arrowRedDownRenderable?.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    /**
     * Реагирует только на первый таб (то есть можно поставить только одну точку)
     * Устанавливает начальные значения треугольнику и отображает стрелку на месте таба
     */
    private fun initOnTabListener() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (triangle?.objectVector == null) {

                createThreeDots(hitResult)

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

                val currentAngle = calculateABAngle(triangle ?: return@setOnTapArPlaneListener)
                showAngle(currentAngle)
            }
        }
    }

    private fun createThreeDots(hitResult: HitResult) {
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

    /**
     * Меняет начальную точки съемки и текущее положение камеры.
     * A также отображает значение угла
     */
    private fun updateAngle() {
        val cameraPose = arFragment.arSceneView?.arFrame?.camera?.pose ?: return

        triangle?.currentCameraVector?.x = cameraPose.tx()

        val angle = calculateABAngle(triangle ?: return)

        if (angle == 90) {
            arFragment.arSceneView.scene.removeChild(anchorNode2)

            val anchor2 = arFragment.arSceneView.session?.createAnchor(cameraPose)

            anchorNode2 = AnchorNode(anchor2).apply {
                renderable = arrowRedDownRenderable
                setParent(arFragment.arSceneView?.scene)
            }

            anchorNode2?.worldPosition?.x = cameraPose.tx()

            triangle?.previousCameraVector?.x = anchorNode2?.worldPosition?.x ?: 0f
        }

        showAngle(angle)
    }

    /**
     * рассчитываем по формуле: угол A = arccos(cos BC), где cos BC =  скалярное произведение векторов на произведение векторов
     *
     * @param triangle Triangle - (положение объекта, начальная точка съемки, текущее положение камеры)
     * @return Int - значение угла в градусах
     */
    private fun calculateABAngle(triangle: Triangle): Int {
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

    private fun showAngle(angle: Int) {
        tvAngle?.text = "angle = $angle"
    }

    /** Add generateFilename method
    A unique file name is needed for each picture we take.
    The filename for the picture is generated using the standard pictures
    directory, and then an album name of Sceneform. Each image name is based
    on the time, so they won't overwrite each other. This path is also related
    to the paths.xml file we added previously.
     */

    private fun generateFilename(): String? {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + File.separator + "Sceneform/" + date + "_screenshot.jpeg"
    }

    /**Add saveBitmapToDisk method
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
        } catch (ex: IOException) {
            Log.e("abracadabra", ex.message.toString())
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    /** Add the takePhoto method
     * The method takePhoto() uses the PixelCopy API to capture a screenshot of the ArSceneView.
     * It is asynchronous since it actually happens between frames. When the listener is called,
     * the bitmap is saved to the disk, and then a snackbar is shown with an intent to open the image
     * in the Pictures application.
     */
    private fun takePhoto() {
        val filename = generateFilename()
        val view: ArSceneView = arFragment.arSceneView

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
                try {
                    saveBitmapToDisk(bitmap, filename ?: "")
                } catch (e: IOException) {
                    val toast = Toast.makeText(
                        this, e.toString(),
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    return@request
                }
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Photo saved", Snackbar.LENGTH_SHORT
                ).show()

            } else {
                val toast = Toast.makeText(
                    this,
                    "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG
                )
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    companion object {
        private const val MIN_OPENGL_VERSION = 3.0
        const val arrowViewSize = 35
    }
}