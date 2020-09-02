package com.arvrlab.reconstructcamera

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    private val singleViewModel: SingleViewModel by viewModels()
    private val permissions = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(permissions,0)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewPager.apply {
            adapter = ViewPagerAdapterAdapter(supportFragmentManager)
            currentItem = 0
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { index, s ->
            if (s == permissions[index] && grantResults[index] == PackageManager.PERMISSION_GRANTED)
                pvPreview.doOnLayout { singleViewModel.cameraX.initCamera(this, it as PreviewView) }
        }
    }
}