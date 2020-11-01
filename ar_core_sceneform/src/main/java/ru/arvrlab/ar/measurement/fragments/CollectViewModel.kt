package ru.arvrlab.ar.measurement.fragments

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import ru.arvrlab.ar.measurement.core.*
import ru.arvrlab.ar.measurement.core.Vector
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class CollectViewModel(private val app: Application) : AndroidViewModel(app) {
    val TAG = "CollectViewModel"

    val toastMessage = MutableLiveData<String>()

    val removeChild = MutableLiveData<AnchorNode>()

    val angleValue = MutableLiveData<Int>()

    val distanceAB = MutableLiveData<Float>()
    val distanceCameraToObj = MutableLiveData<Float>()
    val distanceCameraToFloor = MutableLiveData<Int>()
    val modelAngleFloor = MutableLiveData<Int>()
    var currentModelAngle = Angles.ZERO

    val triangleCamObj = MutableLiveData<Vector3>()
    val angleCamObjVert = MutableLiveData<Int>()

    val currentCameraPos = MutableLiveData<Vector>()
    val currentOrbitNodePos = MutableLiveData<Vector>()

    private var orbitNode: AnchorNode? = null
    private var anchorNode2: AnchorNode? = null
    private var anchorNode3: TransformableNode? = null

    private var triangle: Triangle = Triangle()

    private var tabClicked = false

    private var redSphere: Renderable? = null
    private var blueSphere: Renderable? = null

    /** Add the takePhoto method
     * The method takePhoto() uses the PixelCopy API to capture a screenshot of the ArSceneView.
     * It is asynchronous since it actually happens between frames. When the listener is called,
     * the bitmap is saved to the disk, and then a snackbar is shown with an intent to open the image
     * in the Pictures application.
     */
    fun initRenderable(context: Context) {
        MaterialFactory.makeTransparentWithColor(context, com.google.ar.sceneform.rendering.Color(Color.RED))
            .thenAccept { material: Material? ->
                redSphere = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material) //Vector3.zero() - create x,y,z zero vector
                redSphere?.isShadowCaster = false
                redSphere?.isShadowReceiver = false

            }
            .exceptionally {
                Log.e(TAG, "Init Renderable Error", it)
                return@exceptionally null
            }

        MaterialFactory.makeTransparentWithColor(context, com.google.ar.sceneform.rendering.Color(Color.BLUE))
            .thenAccept { material: Material? ->
                blueSphere = ShapeFactory.makeSphere(
                    0.02f,
                    Vector3.zero(),
                    material
                ) //Vector3.zero() - create x,y,z zero vector
                redSphere?.isShadowCaster = false
                redSphere?.isShadowReceiver = false

            }
            .exceptionally {
                Log.e(TAG, "Init Renderable Error", it)
                return@exceptionally null
            }
    }

    fun takePhoto(view: ArSceneView) {
        val filename = generateFilename()
        val image = view.arFrame?.acquireCameraImage()

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
        currentCameraPos.postValue(Vector(cameraPose.tx(), cameraPose.ty(), cameraPose.tz()))
        triangle.currentCameraVector.x = cameraPose.tx()
        triangle.currentCameraVector.y = cameraPose.ty()
        triangle.currentCameraVector.z = cameraPose.tz()
        measureVertAngleCamToObj(cameraPose)

        return //npe
        val angle = triangle.calculateABAngle()
        angleValue.postValue(angle)


        if (angle in 85..90 && distanceCameraToFloor.value == modelAngleFloor.value && (distanceAB.value!! >= distanceCameraToObj.value!! - 5 && distanceAB.value!! <= distanceCameraToObj.value!! + 5)) {

            takePhoto(arFragment.arSceneView)

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

//            currentModelAngle = when (currentModelAngle) {
//                Angles.ZERO -> Angles.FORTY_FIVE
//                Angles.FORTY_FIVE -> Angles.EIGHTY_FIVE
//                else -> Angles.ZERO
//            }
        }

    }

    private fun measureVertAngleCamToObj(cameraPose: Pose) {
        val ab = measureDistanceFromOrbitNodeToCamera(cameraPose) * 100
        val bc = measureDistanceFromCameraToFloor(cameraPose) * 100
        val ac = measureDistanceFromOrbitNodeToFloor(cameraPose) * 100

        val angle = (acos(cos(ac / ab)) * 180) / PI
        val triangle = Vector3(ab, bc, ac)
        // * 100).toFloat()
        triangleCamObj.postValue(triangle)
        angleCamObjVert.postValue(angle.toInt())

    }

    private fun measureDistanceFromOrbitNodeToCamera(cameraPose: Pose) = calculateDistance(orbitNode?.worldPosition ?: Vector3(), cameraPose)

    // distCamToFloor - katet1
    private fun measureDistanceFromCameraToFloor(cameraPose: Pose): Float
        = orbitNode?.worldPosition?.y?.let { (it - cameraPose.ty()).pow(2) } ?: 0f

    private fun measureDistanceFromOrbitNodeToFloor(cameraPose: Pose): Float{
        val orbitPos = orbitNode?.worldPosition?.let { Vector(it.x, it.y, it.z) } ?: return 0f
        val floor = Vector(cameraPose.tx(),orbitPos.y, cameraPose.tz())
        return calculateDistance(orbitPos,floor)
    }



    fun onTap(hitResult: HitResult, arrowRedDownRenderable: Renderable, arFragment: MyArFragment) {
        if (!tabClicked) {
            tabClicked = true

            createThreeDots(hitResult, arrowRedDownRenderable, arFragment)
        }
    }

    private fun createThreeDots(
        hitResult: HitResult,
        arrowRedDownRenderable: Renderable,
        arFragment: MyArFragment
    ) {
        val orbitAnchor = hitResult.createAnchor()

        var orbitVector: Vector3

        orbitNode = AnchorNode(orbitAnchor).apply {
            renderable = redSphere
            orbitVector = localPosition
            setParent(arFragment.arSceneView?.scene)
        }

        val cameraAnchor = arFragment.arSceneView.session?.createAnchor(arFragment.arSceneView.arFrame?.camera?.pose)

        anchorNode2 = AnchorNode(cameraAnchor).apply {
            renderable = arrowRedDownRenderable
            setParent(arFragment.arSceneView?.scene)
        }
        /*
        arFragment.arSceneView.
            val img = arFragment.arSceneView.arFrame?.acquireCameraImage()
            val image = InputImage.fromMediaImage(img!!, 90)
            image.bitmapInternal
        */
        //provideShift()

    }

fun provideShift(){
    CoroutineScope(Dispatchers.Unconfined).launch {
        var shift = 0f
        while (isActive){
            delay(300)
            shift += 0.01f
            anchorNode3?.localPosition =  anchorNode3?.localPosition?.let {
                Vector3(it.x, it.y + shift, it.z)
            }
            if (shift == 3f) shift = 0f
        }
    }
}

    fun showDistances() {
        val distAB = calculateDistance(triangle.previousCameraVector, triangle.objectVector)
        distanceAB.postValue(distAB)
    }

    private fun calculateDistance(vector1: Vector, vector2: Vector): Float =
        calculateDistance(
            x = vector1.x - vector2.x,
            y = vector1.y - vector2.y,
            z = vector1.z - vector2.z
        )

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Pose) = calculateDistance(
        x = objectPose0.x - objectPose1.tx(),
        y = objectPose0.y - objectPose1.ty(),
        z = objectPose0.z - objectPose1.tz()
    )

    //Euclidean measure
    private fun calculateDistance(x: Float, y: Float, z: Float) = sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}