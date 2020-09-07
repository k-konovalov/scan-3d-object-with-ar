package com.arvrlab.reconstructcamera.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import com.arvrlab.reconstructcamera.core.ManualParametersType
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.scenario_fragment.*
import kotlinx.android.synthetic.main.scenario_fragment.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ScenarioFragment : Fragment(R.layout.scenario_fragment) {

    private val singleViewModel: SingleViewModel by activityViewModels()
    private val activityContext by lazy { requireActivity() }

    private val bar by lazy { Snackbar.make(requireView().coordLayout, "", Snackbar.LENGTH_INDEFINITE) }
    private val snackView by lazy { bar.view as SnackbarLayout }
    private val snack: View by lazy { layoutInflater.inflate(R.layout.snack_bar_view, coordLayout, false) }
    private val snackText: TextView by lazy { snack.findViewById<TextView>(R.id.tvSnackMessage) }
    private val snackProgress: IndicatorSeekBar by lazy { snack.findViewById<IndicatorSeekBar>(R.id.pbSnackProgress) }
    private var currentCapturedPhotoCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCameraXObservers()
        initOnClickListeners()
    }

    private fun initCameraXObservers() {
        val observerForCameraChange = Observer<Any> { _ ->
            activityContext.pvPreview.doOnLayout {
                singleViewModel.cameraX.initCamera(
                    activityContext,
                    it as PreviewView
                )
            }
        }

        singleViewModel.cameraX.run {
            logAndSetupAvailableCameraSettings(requireContext())

            wb.observe(activityContext, observerForCameraChange)
            focus.observe(activityContext, observerForCameraChange)
            iso.observe(activityContext, observerForCameraChange)
            shutter.observe(activityContext, observerForCameraChange)
            frameDuration.observe(activityContext, observerForCameraChange)
            autoExposition.observe(activityContext, observerForCameraChange)
            autoFocus.observe(activityContext, observerForCameraChange)
            autoWB.observe(activityContext, observerForCameraChange)
            flash.observe(activityContext, observerForCameraChange)

            //maxFrameDuration.observe(activityContext, Observer { sbFrameDuration.max =  })
//            errorMessage.observe(activityContext, Observer {
//                showToastWith(it)
//            })
            isBracketingReady.observe(activityContext, Observer {
                if (it && (intervalBetweenShot != 0) && (numPhotos != 0)) {
                    launchBracketing(requireContext(), intervalBetweenShot, numPhotos)
                    showSnackBar()
                }
            })

            capturedPhotoCount.observe(viewLifecycleOwner, Observer {
                updateProgress()
            })
        }
    }

    private fun initOnClickListeners() {
        fabTakePicture.setOnClickListener {
            singleViewModel.cameraX.prepareForBracketing()
            //singleViewModel.cameraX.takePhoto(requireContext())
        }

        fabFirstPhoto.setOnClickListener {
            SetParamsDialogFragment(
                singleViewModel.cameraX.firstPhotoSettings,
                singleViewModel.cameraX
            ) { newParams ->
                singleViewModel.cameraX.firstPhotoSettings = newParams
            }.show(parentFragmentManager, "setParamsDialog")
        }

        fabLastPhoto.setOnClickListener {
            SetParamsDialogFragment(
                singleViewModel.cameraX.lastPhotoSettings,
                singleViewModel.cameraX
            ) { newParams ->
                singleViewModel.cameraX.lastPhotoSettings = newParams
            }.show(parentFragmentManager, "setParamsDialog")
        }
    }

    private fun showSnackBar() {
        currentCapturedPhotoCount = 0
        fabTakePicture.isEnabled = false
        snackProgress.max = singleViewModel.cameraX.numPhotos.toFloat()
        snackText.text = "Подготовка..."
        snackProgress.setProgress(0F)
        snackView.apply {
            addView(snack)
            setBackgroundColor(Color.WHITE)
        }
        bar.show()
    }

    private fun updateProgress() {
        val currentParamValue: String = getCurrentParamValue()
        snackText.text = "\"Брекетинг по параметру \"${singleViewModel.cameraX.differenceParameter.type.name}\". Текущее значение - $currentParamValue"
        snackProgress.setProgress(currentCapturedPhotoCount.toFloat())

        val isLastPhoto = currentCapturedPhotoCount == singleViewModel.cameraX.numPhotos
        if (isLastPhoto) doWhenLastPhoto(currentParamValue)
        else currentCapturedPhotoCount++
    }

    private fun doWhenLastPhoto(currentParamValue: String) {
        CoroutineScope(Dispatchers.Main).launch {
            snackText.text = "\"Брекетинг по параметру \"${singleViewModel.cameraX.differenceParameter.type.name}\". Текущее значение - $currentParamValue"
            snackProgress.setProgress(snackProgress.max)
            delay(700)
            snackView.removeAllViews()
            bar.dismiss()
            fabTakePicture.isEnabled = true
        }
    }

    private fun getCurrentParamValue(): String {
        with(singleViewModel.cameraX) {
            return when (differenceParameter.type) {
                ManualParametersType.SHUTTER -> shutterSpeedsMap.keys.toList()[shutter.value ?: 0]
                ManualParametersType.ISO -> iso.value.toString()
                ManualParametersType.WB -> wb.value.toString()
                ManualParametersType.FOCUS -> focus.value.toString()
                else -> ""
            }
        }
    }

}