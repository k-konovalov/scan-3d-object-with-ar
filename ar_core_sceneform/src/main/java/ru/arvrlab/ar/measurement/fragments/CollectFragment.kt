package ru.arvrlab.ar.measurement.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.collect_fragment.*
import ru.arvrlab.ar.measurement.R

class CollectFragment : Fragment(R.layout.collect_fragment) {

    private val arFragment by lazy { childFragmentManager.findFragmentById(R.id.fragmentSceneform) as MyArFragment }

    private val viewModel: CollectViewModel by viewModels()

    private var arrowRedDownRenderable: Renderable? = null
    val arrowViewSize = 35

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initArrow()
        initListeners()
        initOnClickListeners()
        initObservers()
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
     * Реагирует только на первый таб (то есть можно поставить только одну точку)
     * Устанавливает начальные значения треугольнику и отображает стрелку на месте таба
     */
    private fun initListeners() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, _: MotionEvent? ->
            viewModel.onTap(hitResult, arrowRedDownRenderable ?: return@setOnTapArPlaneListener, arFragment)
        }

        arFragment.arSceneView?.scene?.addOnUpdateListener {
            viewModel.updateAngle(arFragment, arrowRedDownRenderable ?: return@addOnUpdateListener)
            viewModel.measureAngleFromTheFloor()
            viewModel.showDistances()
            viewModel.measureDistanceFromCamera(arFragment)
        }
    }

    private fun initOnClickListeners() {
        btnTakePhoto.setOnClickListener {
            viewModel.takePhoto(arFragment.arSceneView)
        }
    }

    private fun initObservers() {
        viewModel.toastMessage.observe(viewLifecycleOwner, androidx.lifecycle.Observer { toastMessage ->
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
        })

        viewModel.removeChild.observe(viewLifecycleOwner, androidx.lifecycle.Observer {anchorNode ->
            arFragment.arSceneView.scene.removeChild(anchorNode)
        })

        viewModel.angleValue.observe(viewLifecycleOwner, androidx.lifecycle.Observer { angle ->
            tvAngle?.text = "angle = $angle"
        })

        viewModel.currentAngleFloor.observe(viewLifecycleOwner, androidx.lifecycle.Observer { angle ->
            tvCurrentAngleFloor?.text = "current angle from the floor = $angle"
        })

        viewModel.modelAngleFloor.observe(viewLifecycleOwner, androidx.lifecycle.Observer { angle ->
            tvModelAngleFloor?.text = "model angle from the floor = $angle"
        })

        viewModel.distanceAB.observe(viewLifecycleOwner, androidx.lifecycle.Observer { distanceAB ->
            tvDistanceAB.text = (distanceAB * 100).toInt().toString()
        })

        viewModel.distanceAC.observe(viewLifecycleOwner, androidx.lifecycle.Observer { distanceAC ->
            tvDistanceAC.text = "From obj to camera: ${(distanceAC * 100).toInt()}cm"
        })
    }
}