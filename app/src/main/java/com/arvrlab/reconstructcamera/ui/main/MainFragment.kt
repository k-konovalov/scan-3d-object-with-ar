package com.arvrlab.reconstructcamera.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.CustomCameraX
import com.arvrlab.reconstructcamera.R
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment(R.layout.main_fragment) {
    val cameraX = CustomCameraX()
    val permissions = arrayOf("android.permission.CAMERA")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions(permissions,0)

        cameraX.logAndSetupAvailableCameraSettings(requireContext())

        sbWb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cameraX.wb.postValue(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        sbFocus.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cameraX.focus.postValue(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        sbISO.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cameraX.iso.postValue(p1 + 50) // Min ISO always 50
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        sbExposure.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                cameraX.exposure.postValue(p1 + 30)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        val observerForCameraChange = Observer<Int> {
            pvPreview.doOnLayout { cameraX.initCamera(viewLifecycleOwner, it as PreviewView, requireContext()) }
        }

        cameraX.run {
            wb.observe(viewLifecycleOwner, observerForCameraChange)
            focus.observe(viewLifecycleOwner, observerForCameraChange)
            iso.observe(viewLifecycleOwner, observerForCameraChange)
            exposure.observe(viewLifecycleOwner, observerForCameraChange)
            maxFocus.observe(viewLifecycleOwner, Observer { sbFocus.max = it })
            maxIso.observe(viewLifecycleOwner, Observer { sbISO.max = it })
            maxExposure.observe(viewLifecycleOwner, Observer { sbExposure.max = it })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { index, s ->
            if (s == permissions[index] && grantResults[index] == PackageManager.PERMISSION_GRANTED)
                pvPreview.doOnLayout { cameraX.initCamera(viewLifecycleOwner, it as PreviewView, requireContext()) }
        }
    }


}