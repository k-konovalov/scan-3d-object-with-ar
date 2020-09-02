package com.arvrlab.reconstructcamera.ui.main

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File

class MainFragment : Fragment(R.layout.main_fragment) {
    val viewModel: MainViewModel by viewModels()
    val singleViewModel : SingleViewModel by activityViewModels()
    private val permissions = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    private val isPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(requireActivity(), permissions[0]) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireActivity(), permissions[1]) ==
                PackageManager.PERMISSION_GRANTED

    private val outputDirectory: File by lazy { getOutputDir() }

    private fun getOutputDir() : File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if(mediaDir != null && mediaDir.exists()) mediaDir
        else requireActivity().filesDir
    }

    override fun onResume() {
        super.onResume()
        singleViewModel.cameraX.logAndSetupAvailableCameraSettings(requireContext())
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
            singleViewModel.cameraX.takePhoto(outputDirectory, requireContext())
        }
        btnStartTimer.setOnClickListener {
            if (etDelayBetweenPhoto.text.isNotEmpty() && etNumberOfPhotos.text.isNotEmpty())
                singleViewModel.cameraX.initPhotoTimer(requireContext(),etDelayBetweenPhoto.text.toString().toLong(), etNumberOfPhotos.text.toString().toLong() ,outputDirectory)
            else Toast.makeText(requireContext(),"Timer settings is empty?", Toast.LENGTH_SHORT).show()
        }
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

            maxFocus.observe(viewLifecycleOwner, Observer { sbFocus.max = it.toFloat() })
            maxIso.observe(viewLifecycleOwner, Observer { sbISO.max = it.toFloat() })
            maxShutter.observe(viewLifecycleOwner, Observer { sbShutter.max = it.toFloat()})
            //maxFrameDuration.observe(viewLifecycleOwner, Observer { sbFrameDuration.max =  })
//            errorMessage.observe(viewLifecycleOwner, Observer {
//                showToastWith(it)
//            })
        }
    }

    private fun showToastWith(it: String?) {
        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
    }
}