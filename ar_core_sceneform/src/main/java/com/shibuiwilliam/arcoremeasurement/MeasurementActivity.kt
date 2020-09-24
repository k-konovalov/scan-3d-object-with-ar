package com.shibuiwilliam.arcoremeasurement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.AlertDialog
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.view.doOnLayout
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_measurement.*
import com.google.ar.sceneform.rendering.Color as arColor


class MeasurementActivity : AppCompatActivity() {
    /**
     * @see (https://m.habr.com/ru/post/438178/)
     * Глоссарий
     * Сцена: это место, где будут отображаться все ваши 3D-объекты. Эта сцена размещена в AR-фрагменте, который мы добавили в layout.
     * HitResult: это воображаемая линия (или луч), идущая из бесконечности, которая даёт точку пересечения себя с объектом реального мира.
     * Якорь (Anchor): это фиксированное местоположение и ориентация в реальном мире. Его можно понимать как координаты (x, y, z) в трехмерном пространстве.
     * Поза — это положение и ориентация объекта на сцене. Она используется для преобразования локального координатного пространства объекта в реальное координатное пространство.
     * Якорный узел [Node]: это узел, который автоматически позиционирует себя в реальном мире. Это первый узел, который устанавливается при обнаружении плоскости.
     * TransformableNode: это узел, с которым можно взаимодействовать. Его можно перемещать, масштабировать, поворачивать и так далее. В этом примере мы можем масштабировать наш объект и вращать его. Отсюда и название Transformable.*/
    private val TAG: String = MeasurementActivity::class.java.simpleName
    private val viewModel: MeasurementViewModel by viewModels()

    private val arFragment: ArFragment by lazy { (supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment) }

    //Arrows
    private val arrow1UpLinearLayout: LinearLayout by lazy {
        val arrow1UpView = ImageView(this).also { it.setImageResource(R.drawable.arrow_1up) }
        LinearLayout(this).also {
            it.orientation = LinearLayout.VERTICAL
            it.gravity = Gravity.CENTER
            it.addView(arrow1UpView, Constants.arrowViewSize, Constants.arrowViewSize)
        }
    }
    private val arrow1DownLinearLayout: LinearLayout by lazy {
        val arrow1DownView = ImageView(this).also { it.setImageResource(R.drawable.arrow_1down) }
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(arrow1DownView, Constants.arrowViewSize, Constants.arrowViewSize)
        }
    }
    private val arrow10UpLinearLayout: LinearLayout by lazy {
        val arrow10UpView = ImageView(this).also { it.setImageResource(R.drawable.arrow_10up) }
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(arrow10UpView, Constants.arrowViewSize, Constants.arrowViewSize)
        }
    }
    private val arrow10DownLinearLayout: LinearLayout by lazy {
        val arrow10DownView = ImageView(this).also { it.setImageResource(R.drawable.arrow_10down) }
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(arrow10DownView, Constants.arrowViewSize, Constants.arrowViewSize)
        }
    }

    private val distanceModeArrayList by lazy { resources.getStringArray(R.array.distance_mode) }
    private var distanceMode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurement)

        initOnClickListeners()
        configureSpinner()
        initOnTapListeners()
        viewModel.arFragment = arFragment
        viewModel.onSceneUpdateListener = initSceneUpdateListener()

        llGeneral.doOnLayout {
            initRenderable() //(https://github.com/google-ar/sceneform-android-sdk/issues/574)
        }
    }

    private fun initOnClickListeners(){
        btnClear.setOnClickListener { viewModel.clearAllAnchors() }
    }

    private fun initOnTapListeners() {
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            viewModel.run {
                //if (cubeRenderable == null || distanceCardViewRenderable == null) return
                // Creating Anchor.
                when (distanceMode) {
                    distanceModeArrayList[0] -> {
                        clearAllAnchors()
                        placeAnchor(hitResult, distanceCardViewRenderable)
                    }
                    distanceModeArrayList[1] -> {
                        tapDistanceOf2Points(hitResult)
                    }
                    distanceModeArrayList[2] -> {
                        tapDistanceOfMultiplePoints(
                            hitResult,
                            this@MeasurementActivity,
                            ::showErrorAlert
                        )
                    }
                    distanceModeArrayList[3] -> {
                        tapDistanceFromGround(hitResult)
                    }
                    else -> {
                        clearAllAnchors()
                        placeAnchor(hitResult, distanceCardViewRenderable)
                    }
                }
            }
        }
    }

    //For each frame do:...
    private fun initSceneUpdateListener() = Scene.OnUpdateListener {
        viewModel.showCurrentCameraPosition(tvCameraX,tvCameraY,tvCameraZ)
        when(distanceMode) {
            distanceModeArrayList[0] -> viewModel.measureDistanceFromCamera()
            distanceModeArrayList[1] -> viewModel.measureDistanceOf2Points()
            distanceModeArrayList[2] -> viewModel.measureMultipleDistances()
            distanceModeArrayList[3] -> viewModel.measureDistanceFromGround()
            else -> viewModel.measureDistanceFromCamera()
        }
    }

    /**
     * [MaterialFactory] - Utility class used to construct default Materials.
     * [ShapeFactory] - Utility class used to dynamically construct ModelRenderables for various shapes
     * [ViewRenderable] - Renders a 2D Android view in 3D space by attaching it to a [Node] with setRenderable(Renderable). By default, the size of the view is 1 meter in the Scene per 250dp in the layout. Use a [ViewSizer] to control how the size of the view in the Scene is calculated.
     * [ModelRenderable] - Renders a 3D Model by attaching it to a [Node] with setRenderable(Renderable).
     * @see {https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/MaterialFactory?hl=sk}
     * @see {https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/ViewRenderable?hl=sk}
     * @see {https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/ModelRenderable?hl=sk}
     * */
    private fun initRenderable() {
        MaterialFactory.makeTransparentWithColor(this, arColor(Color.RED))
            .thenAccept { material: Material? ->
                viewModel.run {
                    cubeRenderable = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material) //Vector3.zero() - create x,y,z zero vector
                    cubeRenderable.isShadowCaster = false
                    cubeRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(this, R.layout.distance_text_layout)
            .build()
            .thenAccept{
                viewModel.run {
                    distanceCardViewRenderable = it
                    distanceCardViewRenderable.isShadowCaster = false
                    distanceCardViewRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(this, arrow1UpLinearLayout)
            .build()
            .thenAccept{
                viewModel.run {
                    arrow1UpRenderable = it
                    arrow1UpRenderable.isShadowCaster = false
                    arrow1UpRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(this, arrow1DownLinearLayout)
            .build()
            .thenAccept{
                viewModel.run {
                    arrow1DownRenderable = it
                    arrow1DownRenderable.isShadowCaster = false
                    arrow1DownRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(this, arrow10UpLinearLayout)
            .build()
            .thenAccept{
                viewModel.run {
                    arrow10UpRenderable = it
                    arrow10UpRenderable.isShadowCaster = false
                    arrow10UpRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }

        ViewRenderable.builder()
            .setView(this, arrow10DownLinearLayout)
            .build()
            .thenAccept{
                viewModel.run {
                    arrow10DownRenderable = it
                    arrow10DownRenderable.isShadowCaster = false
                    arrow10DownRenderable.isShadowReceiver = false
                }
            }
            .exceptionally {
                showErrorAlert(it)
                return@exceptionally null
            }
    }

    private fun showErrorAlert(it: Throwable){
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(it.message)
            .create()
            .show()
    }

    private fun configureSpinner(){
        distanceMode = distanceModeArrayList[0]
        sDistanceMode.adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, distanceModeArrayList).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        sDistanceMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                distanceMode = (parent as Spinner).selectedItem as String
                viewModel.clearAllAnchors()
                setMode()
                toastMode()
                if (distanceMode == distanceModeArrayList[2]){
                    val layoutParams = tlMultipleDistance.layoutParams
                    layoutParams.height = Constants.multipleDistanceTableHeight
                    tlMultipleDistance.layoutParams = layoutParams
                    initDistanceTable()
                }
                else{
                    val layoutParams = tlMultipleDistance.layoutParams
                    layoutParams.height = 0
                    tlMultipleDistance.layoutParams = layoutParams
                }
                Log.i(TAG, "Selected arcore focus on $distanceMode")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.clearAllAnchors()
                setMode()
                toastMode()
            }
        }
    }

    private fun setMode(){
        tvDistance.text = distanceMode
    }

    private fun toastMode(){
        Toast.makeText(this@MeasurementActivity,
            when(distanceMode){
                distanceModeArrayList[0] -> "Find plane and tap somewhere"
                distanceModeArrayList[1] -> "Find plane and tap 2 points"
                distanceModeArrayList[2] -> "Find plane and tap multiple points"
                distanceModeArrayList[3] -> "Find plane and tap point"
                else -> "???"
            },
            Toast.LENGTH_LONG)
            .show()
    }

    //fill Table Layout
    private fun initDistanceTable(){
        for (i in 0 until Constants.maxNumMultiplePoints+1){
            val tableRow = TableRow(this)
            tlMultipleDistance.addView(tableRow, tlMultipleDistance.width, Constants.multipleDistanceTableHeight / (Constants.maxNumMultiplePoints + 1))
            for (j in 0 until Constants.maxNumMultiplePoints+1){
                val textView = TextView(this)
                textView.setTextColor(Color.WHITE)
                if (i==0){
                    if (j==0){
                        textView.text = "cm"
                    }
                    else{
                        textView.text = (j-1).toString()
                    }
                }
                else{
                    when {
                        j==0 -> {
                            textView.text = (i-1).toString()
                        }
                        i==j -> {
                            textView.text = "-"
                            viewModel.multipleDistances[i-1][j-1] = textView
                        }
                        else -> {
                            textView.text = viewModel.initCM
                            viewModel.multipleDistances[i-1][j-1] = textView
                        }
                    }
                }
                tableRow.addView(textView,
                    tableRow.layoutParams.width / (Constants.maxNumMultiplePoints + 1),
                    tableRow.layoutParams.height)
            }
        }
    }
}