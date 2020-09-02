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
    private val activityContext by lazy { requireActivity() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCameraXObservers()
    }

    private fun initCameraXObservers(){
        val observerForCameraChange = Observer<Any> { _ ->
            activityContext.pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(activityContext, it as PreviewView) }
        }

        singleViewModel.cameraX.run {
            logAndSetupAvailableCameraSettings(activityContext)

            wb.observe(activityContext, observerForCameraChange)
            focus.observe(activityContext, observerForCameraChange)
            iso.observe(activityContext, observerForCameraChange)
            shutter.observe(activityContext, observerForCameraChange)
            frameDuration.observe(activityContext, observerForCameraChange)
            autoExposition.observe(activityContext, observerForCameraChange)
            autoFocus.observe(activityContext, observerForCameraChange)
            autoWB.observe(activityContext, observerForCameraChange)
            flash.observe(activityContext, observerForCameraChange)
        }
    }
}