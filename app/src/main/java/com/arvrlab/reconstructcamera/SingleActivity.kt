package com.arvrlab.reconstructcamera

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.arvrlab.reconstructcamera.core.SharedPrefHelper
import com.arvrlab.reconstructcamera.core.ViewPagerAdapterAdapter
import kotlinx.android.synthetic.main.main_activity.*

class SingleActivity : FragmentActivity(R.layout.main_activity) {

    private val singleViewModel: SingleViewModel by viewModels()
    private val permissions = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    private val isPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(this, permissions[0]) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, permissions[1]) ==
                PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        singleViewModel.sharedPrefHelper = SharedPrefHelper(resources, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        checkPermissionAndInitCamera()
        setupUI()
        initObservers()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { index, s ->
            if (s == permissions[index] && grantResults[index] == PackageManager.PERMISSION_GRANTED)
                pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(this, it as PreviewView) }
        }
    }

    private fun checkPermissionAndInitCamera(){
        if(isPermissionGranted) pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(this, it as PreviewView) }
        else requestPermissions(permissions,0)
    }

    private fun setupUI(){
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewPager.apply {
            adapter = ViewPagerAdapterAdapter(this@SingleActivity)
            currentItem = 0
        }
    }

    private fun initObservers(){
        singleViewModel.cameraX.lastCapturedUri.observe(this, Observer {
            singleViewModel.uploadFileToFtp(this, it)
        })
    }
}