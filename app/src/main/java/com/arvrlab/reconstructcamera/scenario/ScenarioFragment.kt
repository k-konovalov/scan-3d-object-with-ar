package com.arvrlab.reconstructcamera.scenario

import android.os.Bundle
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import kotlinx.android.synthetic.main.main_activity.*

class ScenarioFragment : Fragment(R.layout.scenario_fragment) {

    private val singleViewModel : SingleViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        singleViewModel.cameraX.logAndSetupAvailableCameraSettings(requireContext())
        initCameraXObservers()
    }


    private fun initCameraXObservers(){
        val observerForCameraChange = Observer<Any> { _ ->
            requireActivity().pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(viewLifecycleOwner, it as PreviewView) }
        }

        singleViewModel.cameraX.run {
            logAndSetupAvailableCameraSettings(requireContext())

            wb.observe(viewLifecycleOwner, observerForCameraChange)
            focus.observe(viewLifecycleOwner, observerForCameraChange)
            iso.observe(viewLifecycleOwner, observerForCameraChange)
            shutter.observe(viewLifecycleOwner, observerForCameraChange)
            frameDuration.observe(viewLifecycleOwner, observerForCameraChange)
            autoExposition.observe(viewLifecycleOwner, observerForCameraChange)
            autoFocus.observe(viewLifecycleOwner, observerForCameraChange)
            autoWB.observe(viewLifecycleOwner, observerForCameraChange)
            flash.observe(viewLifecycleOwner, observerForCameraChange)

        }
    }
}