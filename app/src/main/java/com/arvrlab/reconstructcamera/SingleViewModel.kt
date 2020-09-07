package com.arvrlab.reconstructcamera

import androidx.lifecycle.ViewModel
import com.arvrlab.reconstructcamera.core.CustomCameraX

class SingleViewModel: ViewModel() {
    val cameraX = CustomCameraX()
}