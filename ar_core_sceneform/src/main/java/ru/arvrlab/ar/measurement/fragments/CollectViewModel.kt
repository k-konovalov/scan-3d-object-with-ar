package ru.arvrlab.ar.measurement.fragments

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.PixelCopy
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import ru.arvrlab.ar.measurement.core.YuvToRgbConverter
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.arvrlab.ar.measurement.R
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

    //Threads
    val mainScope = CoroutineScope(Dispatchers.Main)
    val handlerThread = HandlerThread("PixelCopier").apply { start() }
    val handler = Handler(handlerThread.looper)
    val toastMessage = MutableLiveData<String>()

    //Angles
    val distanceAB = MutableLiveData<Float>()
    private var triangle: Triangle = Triangle()
    val triangleCamObj = MutableLiveData<Vector3>()
    val angleCamObjVert = MutableLiveData<Int>()
    val currentCameraPos = MutableLiveData<Vector>()
    val currentOrbitNodePos = MutableLiveData<Vector>()

    //Nodes&Renderables
    private var orbitNode: AnchorNode? = null
    val removeChild = MutableLiveData<AnchorNode>()
    private var arrowRedDownRenderable: Renderable? = null
    private var mainObjectTorus: Renderable? = null
    private val orbits: MutableList<Renderable?> = mutableListOf()
    private var correct: Renderable? = null
    private var currentOrbitIndex = 0

    //Tracking
    private var isFirstResult = true
    private var controlDistanceForOrbit = 0
    val controlDistanceForOrbitToUI = MutableLiveData<Int>(0)
    private var tabClicked = false
    var isCameraTracking = true
    private var redCount = 0
    private val step = 100 / 30f
    val redProgress = MutableLiveData<Float>()
    private val correctAnchors = mutableListOf<Node>()
    private var bitmap: Bitmap = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    private var savedBitmap: Bitmap = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    val postedBitmap = MutableLiveData<Bitmap>()

    /**
     * инициальзирует объект Renderable для отображения на сцене
     */
    fun initRenderable(context: Context) {
        ModelRenderable
            .builder()
            .setSource(context, R.raw.orbit_20_4)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                orbits.add(renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                })
            }.exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }

        ModelRenderable
            .builder()
            .setSource(context, R.raw.orbit_45_4)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                orbits.add(renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                })
            }.exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }

        ModelRenderable
            .builder()
            .setSource(context, R.raw.orbit_80_4)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                orbits.add(renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                })
            }.exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }

        ModelRenderable
            .builder()
            .setSource(context, R.raw.correct)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                correct = renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }.exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }

        initArrow(context)
    }

    private fun initArrow(context: Context) {
        val arrowViewSize = 35
        val arrowRedDownLinearLayout = LinearLayout(context).apply {
            val arrowRedDownView = ImageView(context).apply { setImageResource(R.drawable.arrow) }

            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(arrowRedDownView, arrowViewSize, arrowViewSize)
        }

        ViewRenderable
            .builder()
            .setView(context, arrowRedDownLinearLayout)
            .build()
            .thenAccept { renderable ->
                arrowRedDownRenderable = renderable
                arrowRedDownRenderable?.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
            .exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }
    }



    /** Add saveBitmapToDisk method
     * saveBitmapToDisk() writes out the bitmap to the file.
     */
    private fun saveBitmapToDisk(bitmap: Bitmap) {
        val filename = generateFilename()
        val path = generatePath()
        val out = File(path, filename)

        out.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        try {
            FileOutputStream(out.absolutePath).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            toastMessage.postValue(e.toString())
        }
    }

    /** Add generateFilename method
     * A unique file name is needed for each picture we take.
     * The filename for the picture is generated using the standard pictures
     * directory, and then an album name of Sceneform. Each image name is based
     * on the time, so they won't overwrite each other. This path is also related
     * to the paths.xml file we added previously.
     */
    private fun generateFilename() = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date()) + "_screenshot.jpeg"

    private fun generatePath() = app.cacheDir.toString() + "/Sceneform"

    /**
     * Меняет начальную точки съемки и текущее положение камеры.
     * A также отображает значение угла
     */
    fun updateAngle(arFragment: MyArFragment) {
        val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return
        currentCameraPos.postValue(Vector(cameraPose.tx(), cameraPose.ty(), cameraPose.tz()))
        triangle.currentCameraVector.x = cameraPose.tx()
        triangle.currentCameraVector.y = cameraPose.ty()
        triangle.currentCameraVector.z = cameraPose.tz()
        measureVertAngleCamToObj(cameraPose)
        showDistances()

        return //npe
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

    fun onTap(hitResult: HitResult, arFragment: MyArFragment) {
        if (!tabClicked) {
            tabClicked = true
            isCameraTracking = true

            createThreeDots(hitResult, arFragment)
        }
    }

    /**
     * Реагирует только на первый тап (то есть можно поставить только одну точку)
     * Устанавливает начальные значения треугольнику и отображает стрелку на месте тапа
     */
    private fun createThreeDots(hitResult: HitResult, arFragment: MyArFragment) {
        val orbitAnchor = hitResult.createAnchor()
        val transformableNode = initTransformableNode(arFragment)

        orbitNode = AnchorNode(orbitAnchor).apply {
            renderable = mainObjectTorus
            setParent(arFragment.arSceneView?.scene)
            addChild(transformableNode)
        }
    }

    private fun initTransformableNode(arFragment: MyArFragment) = TransformableNode(arFragment.transformationSystem).apply {
        renderable = orbits[currentOrbitIndex++]
        scaleController.isEnabled = true
        scaleController.minScale = 0.29f
        scaleController.maxScale = 0.3f
        translationController.isEnabled = false
        rotationController.isEnabled = false
    }

    fun checkRed(arFragment: MyArFragment){
        //Log.e("Pixel", "startRequest")
        if(bitmap.width == 1) bitmap = Bitmap.createBitmap(
            arFragment.arSceneView.width,
            arFragment.arSceneView.height,
            Bitmap.Config.ARGB_8888
        )

        PixelCopy.request(
            arFragment.arSceneView.holder.surface,
            bitmap,
            realCheckRed(arFragment),
            handler
        )
    }

    private fun realCheckRed(arFragment: MyArFragment) = { copyResult: Int ->
        if (copyResult == PixelCopy.SUCCESS) {
            val smallBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * 0.1).toInt(),
                (bitmap.height * 0.1).toInt(),
                false
            )
            val redPixels = countRedPixels(smallBitmap)
            val allPixels = smallBitmap.height * smallBitmap.width
            val percentOfRed = (redPixels.toFloat() / allPixels.toFloat())
            redProgress.postValue(percentOfRed)
            if (percentOfRed * 100 > 40) {
                Log.e("Pixel", "All:$allPixels %Red:${percentOfRed.toInt()} Red:$redPixels")
                if (isCameraTracking) mainScope.launch {
                    if (redCount < 30) {
                        redCount++
                    } else addPhotoCapturedAnchor(arFragment)
                }
            }
        }
    }

    private fun countRedPixels(smallBitmap: Bitmap): Int {
        var redPixels = 0
        for (x in 0 until smallBitmap.width) {
            for (y in 0 until smallBitmap.height) {
                val pixel = smallBitmap.getPixel(x, y)
                val hsv = FloatArray(3)
                Color.RGBToHSV(
                    Color.red(pixel),
                    Color.green(pixel),
                    Color.blue(pixel),
                    hsv
                )
                val hue = hsv[0]
                val value = hsv[2]

                val isPixelRed = (hue > 335f || hue < 25f) && value in 0.1f..0.95f
                if (isPixelRed) redPixels++
            }
        }
        return redPixels
    }

    private fun addPhotoCapturedAnchor(arFragment: MyArFragment){
        redCount = 0
        val cameraAnchor = arFragment.arSceneView.session?.createAnchor(arFragment.arSceneView.arFrame?.camera?.pose)
        val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose ?: return
        if (isFirstResult) {
            isFirstResult = !isFirstResult
            controlDistanceForOrbit = (measureDistanceFromOrbitNodeToCamera(cameraPose) * 100).toInt()
            controlDistanceForOrbitToUI.postValue(controlDistanceForOrbit)
        }
        val testRange = (measureDistanceFromOrbitNodeToCamera(cameraPose) * 100).toInt()
        if (correctAnchors.size != 4 && (testRange in controlDistanceForOrbit - 2..controlDistanceForOrbit + 2)) {
            val zStridedNode = TransformableNode(arFragment.transformationSystem).apply {
                scaleController.minScale = 0.04f
                scaleController.maxScale = 0.10f

                scaleController.isEnabled = true

                renderable = correct
                setParent(arFragment.arSceneView?.scene)
                val camPos = cameraAnchor?.pose?.let { Vector3(it.tx(), it.ty(), it.tz()) }
                val orbitPos = orbitNode?.worldPosition ?: Vector3(0f, 0f, 0f)
                val avgPos = camPos?.let {
                    Vector3(
                        listOf(it.x, orbitPos.x).average().toFloat(),
                        listOf(it.y, orbitPos.y).average().toFloat(),
                        listOf(it.z, orbitPos.z).average().toFloat()
                    )
                }
                worldPosition = avgPos
            }
            /*val anchor = AnchorNode(cameraAnchor).apply {
                worldPosition = cameraAnchor?.pose?.let { Vector3(it.tx(), it.ty(), it.tz() - 5f) }
                renderable = arrowRedDownRenderable
                setParent(arFragment.arSceneView?.scene)
            }*/
            redCount = 0
            saveImage(arFragment)
            correctAnchors.add(zStridedNode)
            toastMessage.postValue("Captured")
        }
        if (correctAnchors.size == 4) {
            correctAnchors.run {
                forEach { arFragment.arSceneView.scene?.removeChild(it) }
                clear()
            }
            addNextOrbit(arFragment)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getColorWith(percentOfRed: Float) = Color.argb(percentOfRed,1f,0f,0f)

    private fun addNextOrbit(arFragment: MyArFragment) {
        if (currentOrbitIndex < 3) {
            orbitNode?.run {
                children?.forEach { removeChild(it) }
                addChild(initTransformableNode(arFragment))
            }
        }
    }

    private fun saveImage(arFragment: MyArFragment) {
        CoroutineScope(Dispatchers.Default).launch {
            arFragment.arSceneView.arFrame?.acquireCameraImage()?.use {img ->
                savedBitmap = YuvToRgbConverter.yuvToRgb(img) ?: return@launch
                saveBitmapToDisk(savedBitmap)
                postedBitmap.postValue(savedBitmap)
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