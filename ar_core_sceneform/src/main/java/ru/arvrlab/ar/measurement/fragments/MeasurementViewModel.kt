package ru.arvrlab.ar.measurement.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import ru.arvrlab.ar.measurement.R
import ru.arvrlab.ar.measurement.core.Constants
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementViewModel : ViewModel() {
    private val TAG = "MeasurementViewModel"
    lateinit var arFragment: ArFragment

    lateinit var arrow1UpRenderable: Renderable
    lateinit var arrow1DownRenderable: Renderable
    lateinit var arrow10UpRenderable: Renderable
    lateinit var arrow10DownRenderable: Renderable
    lateinit var cubeRenderable: ModelRenderable
    lateinit var distanceCardViewRenderable: ViewRenderable

    lateinit var onSceneUpdateListener: Scene.OnUpdateListener

    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val midAnchors: MutableMap<String, Anchor> = mutableMapOf()
    private val midAnchorNodes: MutableMap<String, AnchorNode> = mutableMapOf()
    private val fromGroundNodes = ArrayList<List<Node>>()

    val multipleDistances = Array(Constants.maxNumMultiplePoints) { Array<TextView?>(Constants.maxNumMultiplePoints) { null } }
    val initCM: String = "0.0"

    fun clearAllAnchors(){
        placedAnchors.clear()
        for (anchorNode in placedAnchorNodes){
            arFragment.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor?.detach()
            anchorNode.setParent(null)
        }
        placedAnchorNodes.clear()
        midAnchors.clear()
        for ((k,anchorNode) in midAnchorNodes){
            arFragment.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor?.detach()
            anchorNode.setParent(null)
        }
        midAnchorNodes.clear()
        for (i in 0 until Constants.maxNumMultiplePoints){
            for (j in 0 until Constants.maxNumMultiplePoints){
                if (multipleDistances[i][j] != null){
                    multipleDistances[i][j]?.text = if(i==j) "-" else initCM
                }
            }
        }
        fromGroundNodes.clear()
    }

    fun tapDistanceFromGround(hitResult: HitResult){
        clearAllAnchors()
        val anchor = hitResult.createAnchor()
        placedAnchors.add(anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val transformableNode = TransformableNode(arFragment.transformationSystem).apply{
            rotationController.isEnabled = false
            scaleController.isEnabled = false
            translationController.isEnabled = true
            renderable = renderable
            setParent(anchorNode)
        }

        val node = Node().apply {
            setParent(transformableNode)
            worldPosition = Vector3(
                anchorNode.worldPosition.x,
                anchorNode.worldPosition.y,
                anchorNode.worldPosition.z)
            renderable = distanceCardViewRenderable
        }

        val arrow1UpNode = Node().apply {
            setParent(node)
            worldPosition = Vector3(
                node.worldPosition.x,
                node.worldPosition.y+0.1f,
                node.worldPosition.z
            )
            renderable = arrow1UpRenderable
            setOnTapListener { hitTestResult, motionEvent ->
                node.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y+0.01f,
                    node.worldPosition.z
                )
            }
        }

        val arrow1DownNode = Node().apply {
            setParent(node)
            worldPosition = Vector3(
                node.worldPosition.x,
                node.worldPosition.y - 0.08f,
                node.worldPosition.z
            )
            renderable = arrow1DownRenderable
            setOnTapListener { hitTestResult, motionEvent ->
                node.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y - 0.01f,
                    node.worldPosition.z
                )
            }
        }

        val arrow10UpNode = Node().apply {
            setParent(node)
            worldPosition = Vector3(
                node.worldPosition.x,
                node.worldPosition.y + 0.18f,
                node.worldPosition.z
            )
            renderable = arrow10UpRenderable
            setOnTapListener { hitTestResult, motionEvent ->
                node.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y + 0.1f,
                    node.worldPosition.z
                )
            }
        }

        val arrow10DownNode = Node().apply {
            setParent(node)
            worldPosition = Vector3(
                node.worldPosition.x,
                node.worldPosition.y - 0.167f,
                node.worldPosition.z
            )
            renderable = arrow10DownRenderable
            setOnTapListener { hitTestResult, motionEvent ->
                node.worldPosition = Vector3(
                    node.worldPosition.x,
                    node.worldPosition.y - 0.1f,
                    node.worldPosition.z
                )
            }
        }

        fromGroundNodes.add(listOf(node, arrow1UpNode, arrow1DownNode, arrow10UpNode, arrow10DownNode))

        arFragment.arSceneView.scene.run {
            addOnUpdateListener(onSceneUpdateListener)
            addChild(anchorNode)
        }
        transformableNode.select()
    }

    fun placeAnchor(hitResult: HitResult, renderable: Renderable){
        val anchor = hitResult.createAnchor()
        placedAnchors.add(anchor)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment.arSceneView.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val node = TransformableNode(arFragment.transformationSystem)
            .apply{
                rotationController.isEnabled = false
                scaleController.isEnabled = false
                translationController.isEnabled = true
                this.renderable = renderable
                setParent(anchorNode)
            }

        arFragment.arSceneView.scene.run {
            addOnUpdateListener(onSceneUpdateListener)
            addChild(anchorNode)
        }
        node.select()
    }

    fun tapDistanceOf2Points(hitResult: HitResult){
        when (placedAnchorNodes.size) {
            0 -> placeAnchor(hitResult, cubeRenderable)
            1 -> {
                placeAnchor(hitResult, cubeRenderable)

                val midPosition = floatArrayOf(
                    (placedAnchorNodes[0].worldPosition.x + placedAnchorNodes[1].worldPosition.x) / 2,
                    (placedAnchorNodes[0].worldPosition.y + placedAnchorNodes[1].worldPosition.y) / 2,
                    (placedAnchorNodes[0].worldPosition.z + placedAnchorNodes[1].worldPosition.z) / 2
                )
                val quaternion = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
                val pose = Pose(midPosition, quaternion)

                placeMidAnchor(pose, distanceCardViewRenderable)
            }
            else -> {
                clearAllAnchors()
                placeAnchor(hitResult, cubeRenderable)
            }
        }
    }

    private fun placeMidAnchor(pose: Pose, renderable: Renderable, between: Array<Int> = arrayOf(0,1)){
        val midKey = "${between[0]}_${between[1]}"
        val session = arFragment.arSceneView.session ?: return
        val anchor = session.createAnchor(pose)
        midAnchors[midKey] = anchor

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment.arSceneView.scene)
        }
        midAnchorNodes[midKey] = anchorNode

        val node = TransformableNode(arFragment.transformationSystem)
            .apply{
                rotationController.isEnabled = false
                scaleController.isEnabled = false
                translationController.isEnabled = true
                this.renderable = renderable
                setParent(anchorNode)
            }
        arFragment.arSceneView.scene.run {
            addOnUpdateListener(onSceneUpdateListener)
            addChild(anchorNode)
        }
    }

    fun tapDistanceOfMultiplePoints(hitResult: HitResult, context: Context, showErrorAlert: (msg: Throwable) -> Unit){
        if (placedAnchorNodes.size >= Constants.maxNumMultiplePoints){
            clearAllAnchors()
        }
        ViewRenderable
            .builder()
            .setView(context, R.layout.point_text_layout)
            .build()
            .thenAccept{
                it.isShadowReceiver = false
                it.isShadowCaster = false
                (it.view as TextView).text = placedAnchors.size.toString() // pointTextView
                placeAnchor(hitResult, it)
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }
        Log.i(TAG, "Number of anchors: ${placedAnchorNodes.size}")
    }

    @SuppressLint("SetTextI18n")
    fun measureDistanceFromGround(){
        if (fromGroundNodes.size == 0) return
        for (node in fromGroundNodes){
            val textView = (distanceCardViewRenderable.view as LinearLayout)
                .findViewById<TextView>(R.id.distanceCard)
            val distanceCM = changeUnit(node[0].worldPosition.y + 1.0f, "cm")
            textView.text = "%.0f".format(distanceCM) + " cm"
        }
    }

    fun showCurrentCameraPosition(tvCameraX: TextView, tvCameraY: TextView, tvCameraZ: TextView) {
        val frame = arFragment.arSceneView.arFrame ?: return

        frame.camera.pose?.run {
            tvCameraX.text = tx().toString()
            tvCameraY.text = ty().toString()
            tvCameraZ.text = tz().toString()
        }
    }

    fun measureDistanceFromCamera() {
        val frame = arFragment.arSceneView.arFrame ?: return
        if (placedAnchorNodes.size >= 1) {
            val distanceMeter = calculateDistance(placedAnchorNodes[0].worldPosition, frame.camera.pose)
            measureDistanceOf2Points(distanceMeter)
        }
    }

    fun measureDistanceOf2Points(){
        if (placedAnchorNodes.size == 2) {
            val distanceMeter = calculateDistance(placedAnchorNodes[0].worldPosition, placedAnchorNodes[1].worldPosition)
            measureDistanceOf2Points(distanceMeter)
        }
    }

    private fun measureDistanceOf2Points(distanceMeter: Float){
        val distanceTextCM = makeDistanceTextWithCM(distanceMeter)
        val textView = (distanceCardViewRenderable.view as LinearLayout)
            .findViewById<TextView>(R.id.distanceCard)
        textView.text = distanceTextCM
        Log.d(TAG, "distance: ${distanceTextCM}")
    }

    fun measureMultipleDistances(){
        if (placedAnchorNodes.size > 1){
            for (i in 0 until placedAnchorNodes.size){
                for (j in i+1 until placedAnchorNodes.size){
                    val distanceMeter = calculateDistance(
                        placedAnchorNodes[i].worldPosition,
                        placedAnchorNodes[j].worldPosition)
                    val distanceCM = changeUnit(distanceMeter, "cm")
                    val distanceCMFloor = "%.2f".format(distanceCM)
                    multipleDistances[i][j]?.text = distanceCMFloor
                    multipleDistances[j][i]?.text = distanceCMFloor
                }
            }
        }
    }

    private fun makeDistanceTextWithCM(distanceMeter: Float): String{
        val distanceCMFloor = changeUnit(distanceMeter, "cm")
        //val distanceCMFloor = "%.2f".format(distanceCM)
        return "$distanceCMFloor cm"
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Pose): Float{
        return calculateDistance(
            objectPose0.x - objectPose1.tx(),
            objectPose0.y - objectPose1.ty(),
            objectPose0.z - objectPose1.tz()
        )
    }

    private fun calculateDistance(objectPose0: Vector3, objectPose1: Vector3): Float{
        return calculateDistance(
            objectPose0.x - objectPose1.x,
            objectPose0.y - objectPose1.y,
            objectPose0.z - objectPose1.z
        )
    }

    //Euclidean measure
    private fun calculateDistance(x: Float, y: Float, z: Float) = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

    private fun calculateDistance(objectPose0: Pose, objectPose1: Pose): Float{
        return calculateDistance(
            objectPose0.tx() - objectPose1.tx(),
            objectPose0.ty() - objectPose1.ty(),
            objectPose0.tz() - objectPose1.tz())
    }

    private fun changeUnit(distanceMeter: Float, unit: String) = when(unit){
        "cm" -> distanceMeter * 100
        "mm" -> distanceMeter * 1000
        else -> distanceMeter
    }
}