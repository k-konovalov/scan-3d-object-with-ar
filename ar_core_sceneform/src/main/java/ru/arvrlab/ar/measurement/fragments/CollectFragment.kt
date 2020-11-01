package ru.arvrlab.ar.measurement.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.BaseArFragment
import kotlinx.android.synthetic.main.collect_fragment.*
import kotlinx.coroutines.*
import ru.arvrlab.ar.measurement.R

class CollectFragment : Fragment(R.layout.collect_fragment) {

    private val arFragment by lazy { childFragmentManager.findFragmentById(R.id.fragmentSceneform) as MyArFragment}

    private val viewModel: CollectViewModel by viewModels()

    private var arrowRedDownRenderable: Renderable? = null
    val arrowViewSize = 35
    val bitmap by lazy { Bitmap.createBitmap(
        arFragment.arSceneView.width,
        arFragment.arSceneView.height,
        Bitmap.Config.ARGB_8888
    ) }

    val handlerThread = HandlerThread("PixelCopier").apply {
        start()
    }
    val handler = Handler(handlerThread.looper)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initArrow()
        initListeners()
        initOnClickListeners()
        initObservers()
        viewModel.initRenderable(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }

    /**
     * инициальзирует объект Renderable для отображения на сцене
     */
    private fun initArrow() {
        val arrowRedDownLinearLayout = LinearLayout(requireContext()).apply {
            val arrowRedDownView = ImageView(context).apply { setImageResource(R.drawable.arrow) }

            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(arrowRedDownView, arrowViewSize, arrowViewSize)
        }

        ViewRenderable
            .builder()
            .setView(requireContext(), arrowRedDownLinearLayout)
            .build()
            .thenAccept { renderable ->
                arrowRedDownRenderable = renderable
                arrowRedDownRenderable?.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
            .exceptionally {
                AlertDialog.Builder(requireContext()).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }


    }

    /**
     * Реагирует только на первый тап (то есть можно поставить только одну точку)
     * Устанавливает начальные значения треугольнику и отображает стрелку на месте тапа
     */
    private fun initListeners() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, _: MotionEvent? ->
            viewModel.onTap(
                hitResult,
                arrowRedDownRenderable ?: return@setOnTapArPlaneListener,
                arFragment
            )
        }

        arFragment.arSceneView?.scene?.addOnUpdateListener {
            viewModel.updateAngle(arFragment, arrowRedDownRenderable ?: return@addOnUpdateListener)
            viewModel.showDistances()
            try { checkRed() } catch (e:Exception) {}
        }
    }

    private fun initOnClickListeners() {
        /*btnTakePhoto.setOnClickListener {
            viewModel.takePhoto(arFragment.arSceneView)
        }*/
    }

    private fun initObservers() {
        val textObserver = Observer<Float>{
            //Todo: To better way
        }
        viewModel.toastMessage.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { toastMessage ->
                Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
            })

        viewModel.removeChild.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { anchorNode ->
                arFragment.arSceneView.scene.removeChild(anchorNode)
            })

        viewModel.triangleCamObj.observe(viewLifecycleOwner, Observer {
            tvCamObjGipotenuza?.text = "${it.x.toInt()}"
            tvCamObjKatet1?.text = "${it.y.toInt()}"
            tvCamObjKatet2?.text = "${it.z.toInt()}"
        })

        viewModel.angleCamObjVert.observe(viewLifecycleOwner, Observer {
            tvCameraObjVertAngle.text = it.toString()
        })

        viewModel.currentCameraPos.observe(viewLifecycleOwner, Observer {
            txtCamX.text = it.x.toString()
            txtCamY.text = it.y.toString()
            txtCamZ.text = it.z.toString()
        })

        viewModel.currentOrbitNodePos.observe(viewLifecycleOwner, Observer {
            txtOrbitNodeX.text = it.x.toString()
            txtOrbitNodeY.text = it.y.toString()
            txtOrbitNodeZ.text = it.z.toString()
        })

    }

    private fun checkRed(){
        //Log.e("Pixel", "startRequest")
        PixelCopy.request(
            arFragment.arSceneView.holder.surface,
            bitmap,
            realCheckRed(),
            handler
        )
    }

    private fun realCheckRed() = { copyResult: Int ->
            if (copyResult == PixelCopy.SUCCESS) {
                val smallBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * 0.1).toInt(),
                    (bitmap.height * 0.1).toInt(),
                    false
                )
                val redPixels = countRedPixels(smallBitmap)
                val allPixels = smallBitmap.height * smallBitmap.width
                val percentOfRed = (redPixels.toFloat() / allPixels.toFloat()) * 100
                if (percentOfRed > 50)
                    Log.e(
                        "Pixel",
                        "All:$allPixels %Red:${percentOfRed.toInt()} Red:$redPixels"
                    )
            }

            //handlerThread.quitSafely()
        }
    fun countRedPixels(smallBitmap: Bitmap): Int {
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
}