package com.arvrlab.reconstructcamera.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.CustomCameraX
import com.arvrlab.reconstructcamera.R
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment(R.layout.main_fragment) {
    val cameraX = CustomCameraX()
    val permissions = arrayOf("android.permission.CAMERA")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions(permissions,0)

        cameraX.logAndSetupAvailableCameraSettings(requireContext())

        initCameraXObservers()
        initSeekbarsListeners()
    }

    private fun initSeekbarsListeners(){
        sbWb.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                cameraX.wb.postValue(seekParams?.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbFocus.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                cameraX.focus.postValue(seekParams?.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbISO.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                cameraX.iso.postValue(seekParams?.progress?.plus(50)) // Min ISO always 50
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbShutter.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                cameraX.shutter.postValue(seekParams?.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbFrameDuration.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                //cameraX.frameDuration.postValue(p1)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
    }

    private fun initCameraXObservers(){
        val observerForCameraChange = Observer<Int> {
            pvPreview.doOnLayout { cameraX.initCamera(viewLifecycleOwner, it as PreviewView, requireContext()) }
        }

        cameraX.run {
            logAndSetupAvailableCameraSettings(requireContext())

            wb.observe(viewLifecycleOwner, observerForCameraChange)
            focus.observe(viewLifecycleOwner, observerForCameraChange)
            iso.observe(viewLifecycleOwner, observerForCameraChange)
            shutter.observe(viewLifecycleOwner, observerForCameraChange)
            frameDuration.observe(viewLifecycleOwner, observerForCameraChange)

            maxFocus.observe(viewLifecycleOwner, Observer { sbFocus.max = it.toFloat() })
            maxIso.observe(viewLifecycleOwner, Observer { sbISO.max = it.toFloat() })
            maxShutter.observe(viewLifecycleOwner, Observer { sbShutter.max = it.toFloat()})
            //maxFrameDuration.observe(viewLifecycleOwner, Observer { sbFrameDuration.max =  })
            errorMessage.observe(viewLifecycleOwner, Observer {
                showToastWith(it)
            })
        }
    }

    private fun showToastWith(it: String?) {
        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { index, s ->
            if (s == permissions[index] && grantResults[index] == PackageManager.PERMISSION_GRANTED)
                pvPreview.doOnLayout { cameraX.initCamera(viewLifecycleOwner, it as PreviewView, requireContext()) }
        }
    }


}