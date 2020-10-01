package com.test.measurement

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.os.*
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_measurement.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementActivity : AppCompatActivity(R.layout.activity_measurement) {

    private val arFragment by lazy { supportFragmentManager.findFragmentById(R.id.fragmentSceneform) as MyArFragment }

    private val viewModel: MeasurementViewModel by viewModels()

    private var arrowRedDownRenderable: Renderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkIsSupportedDeviceOrFinish()
        initArrow()
        initListeners()
        initOnClickListeners()
        initObservers()
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
    private fun initListeners() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, _: MotionEvent? ->
          viewModel.onTap(hitResult, arrowRedDownRenderable ?: return@setOnTapArPlaneListener, arFragment)
        }

        arFragment.arSceneView?.scene?.addOnUpdateListener {
            viewModel.updateAngle(arFragment, arrowRedDownRenderable ?: return@addOnUpdateListener)
        }
    }

    private fun initOnClickListeners() {
        btnTakePhoto.setOnClickListener {
            viewModel.takePhoto(arFragment.arSceneView)
        }
    }

    private fun initObservers() {
        viewModel.toastMessage.observe(this, androidx.lifecycle.Observer { toastMessage ->
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        })

        viewModel.removeChild.observe(this, androidx.lifecycle.Observer {anchorNode ->
            arFragment.arSceneView.scene.removeChild(anchorNode)
        })

        viewModel.angleValue.observe(this, androidx.lifecycle.Observer { angle ->
            tvAngle?.text = "angle = $angle"
        })


    }

    companion object {
        private const val MIN_OPENGL_VERSION = 3.0
        const val arrowViewSize = 35
    }
}