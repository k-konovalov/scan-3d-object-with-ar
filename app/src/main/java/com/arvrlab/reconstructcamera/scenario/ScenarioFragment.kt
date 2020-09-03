package com.arvrlab.reconstructcamera.scenario

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.scenario_fragment.*

class ScenarioFragment : Fragment(R.layout.scenario_fragment) {

    private val singleViewModel: SingleViewModel by activityViewModels()
    private val activityContext by lazy { requireActivity() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCameraXObservers()
        initSwitchListeners()
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
        }
    }

    private fun initSwitchListeners() {
        sAF.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) singleViewModel.cameraX.autoFocus.postValue(true)
            else singleViewModel.cameraX.autoFocus.postValue(false)
        }
        sAutoIsoShutter.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) singleViewModel.cameraX.autoExposition.postValue(true)
            else singleViewModel.cameraX.autoExposition.postValue(false)
        }
        sAutoWB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) singleViewModel.cameraX.autoWB.postValue(true)
            else singleViewModel.cameraX.autoWB.postValue(false)
        }
        sFlash.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) singleViewModel.cameraX.flash.postValue(true)
            else singleViewModel.cameraX.flash.postValue(false)
        }
    }

    private fun initOnClickListeners() {
        fabTakePicture.setOnClickListener {
            singleViewModel.cameraX.takePhoto(requireContext())
        }

        fabFirstPhoto.setOnClickListener {
            SetParamsDialogFragment(singleViewModel.cameraX.firstPhotoSettings).show(parentFragmentManager, "setParamsDialog")

            fabLastPhoto.setOnClickListener {
                SetParamsDialogFragment (singleViewModel.cameraX.lastPhotoSettings).show(parentFragmentManager, "setParamsDialog")
            }
        }
    }
}