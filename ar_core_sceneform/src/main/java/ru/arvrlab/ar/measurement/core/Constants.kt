package ru.arvrlab.ar.measurement.core

import android.Manifest

object Constants{
    const val maxNumMultiplePoints = 6
    const val multipleDistanceTableHeight = 300

    const val arrowViewSize = 45

    val permissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE)
}