package com.arvrlab.reconstructcamera.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_fragment.*

class CameraFragment : Fragment(R.layout.main_fragment) {

    private val singleViewModel : SingleViewModel by activityViewModels()
    private val activityContext by lazy { requireActivity() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCameraXObservers()
        initSeekbarsListeners()
        initSwitchListeners()
        initOnClickListeners()
    }

    private fun initSeekbarsListeners(){
        sbWb.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                singleViewModel.cameraX.wb.postValue(seekParams?.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbFocus.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                singleViewModel.cameraX.focus.postValue(seekParams?.progress)
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbISO.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                singleViewModel.cameraX.iso.postValue(seekParams?.progress?.plus(50)) // Min ISO always 50
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        sbShutter.onSeekChangeListener = object : OnSeekChangeListener{
            override fun onSeeking(seekParams: SeekParams?) {
                singleViewModel.cameraX.shutter.postValue(seekParams?.progress)
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

    private fun initSwitchListeners() {
        sAF.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) singleViewModel.cameraX.autoFocus.postValue(true)
            else singleViewModel.cameraX.autoFocus.postValue(false)
        }
        sAutoIsoShutter.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) singleViewModel.cameraX.autoExposition.postValue(true)
            else singleViewModel.cameraX.autoExposition.postValue(false)
        }
        sAutoWB.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) singleViewModel.cameraX.autoWB.postValue(true)
            else singleViewModel.cameraX.autoWB.postValue(false)
        }
        sFlash.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) singleViewModel.cameraX.flash.postValue(true)
            else singleViewModel.cameraX.flash.postValue(false)
        }
    }

    private fun initOnClickListeners() {
        fabTakePicture.setOnClickListener {
            singleViewModel.cameraX.takePhoto(requireContext())
        }
        btnStartTimer.setOnClickListener {
            if (etDelayBetweenPhoto.text.isNotEmpty() && etNumberOfPhotos.text.isNotEmpty())
                singleViewModel.cameraX.initPhotoTimer(requireContext(),etDelayBetweenPhoto.text.toString().toLong(), etNumberOfPhotos.text.toString().toLong())
            else Toast.makeText(requireContext(),"Timer settings is empty?", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initCameraXObservers(){
        val observerForCameraChange = Observer<Any> { _ ->
            requireActivity().pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(activityContext, it as PreviewView) }
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

            maxFocus.observe(activityContext, Observer { sbFocus.max = it.toFloat() })
            maxIso.observe(activityContext, Observer { sbISO.max = it.toFloat() })
            maxShutter.observe(activityContext, Observer { sbShutter.max = it.toFloat()})
            //maxFrameDuration.observe(activityContext, Observer { sbFrameDuration.max =  })
//            errorMessage.observe(activityContext, Observer {
//                showToastWith(it)
//            })
        }
    }

    private fun showToastWith(it: String?) {
        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
    }
}