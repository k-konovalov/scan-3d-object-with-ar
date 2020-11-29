package ru.arvrlab.ar.measurement.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.BaseArFragment
import kotlinx.android.synthetic.main.collect_fragment.*
import kotlinx.coroutines.*
import ru.arvrlab.ar.measurement.R

class CollectFragment : Fragment(R.layout.collect_fragment) {

    private val viewModel: CollectViewModel by viewModels()
    private val arFragment by lazy { childFragmentManager.findFragmentById(R.id.fragmentSceneform) as MyArFragment }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        initOnClickListeners()
        initObservers()
        viewModel.initRenderable(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.handlerThread.quitSafely()
    }

    private fun initListeners() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, _: MotionEvent? ->
            viewModel.onTap(hitResult, arFragment)
        }

        arFragment.arSceneView?.scene?.addOnUpdateListener {
            viewModel.updateAngle(arFragment)
            try { viewModel.checkRed(arFragment) } catch (e:Exception) {}
        }
    }

    private fun initOnClickListeners() {
        /*btnTakePhoto.setOnClickListener {
            viewModel.takePhoto(arFragment.arSceneView)
        }*/
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initObservers() {
        val textObserver = Observer<Float>{
            //Todo: To better way
        }
        viewModel.run {
            toastMessage.observe(viewLifecycleOwner, Observer { toastMessage ->
                Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
            })

            removeChild.observe(viewLifecycleOwner, Observer { anchorNode ->
                arFragment.arSceneView.scene.removeChild(anchorNode)
            })

            redProgress.observe(viewLifecycleOwner, Observer {
                tvRedCount.text = if (it * 2.3 <= 1.0) (it * 2.3).toString().substring(0, 3) else "1.0"
                tvRedCount.background.setColorFilter(
                    viewModel.getColorWith(it * 2),
                    PorterDuff.Mode.SRC_ATOP
                )
            })

            controlDistanceForOrbitToUI.observe(viewLifecycleOwner, Observer {
                tvCurrentFixedDistance.text = it.toString()
            })

            triangleCamObj.observe(viewLifecycleOwner, Observer {
                tvCamObjGipotenuza?.text = "${it.x.toInt()}"
                tvCamToObjDistance?.text = "${it.x.toInt()}"
                tvCamObjKatet1?.text = "${it.y.toInt()}"
                tvCamObjKatet2?.text = "${it.z.toInt()}"
            })

            angleCamObjVert.observe(viewLifecycleOwner, Observer {
                tvCameraObjVertAngle.text = it.toString()
            })
            postedBitmap.observe(viewLifecycleOwner, Observer {
                llCarouselImg.addView(
                    ImageView(requireContext()).apply {
                        adjustViewBounds = true
                        maxWidth = 300
                        setImageBitmap(it)
                    }
                )
            })



        }
    }
}