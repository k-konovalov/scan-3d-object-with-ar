package com.shibuiwilliam.measurement

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlin.system.exitProcess

class MyApp: Application() {
    private val MIN_OPENGL_VERSION = 3.0
    private val TAG = "MyApp"

    override fun onCreate() {
        super.onCreate()
        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
            exitProcess(0)
        }
    }

    private fun checkIsSupportedDeviceOrFinish(context: Context): Boolean {
        val openGlVersionString = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} later")
            Toast.makeText(context, "Sceneform requires OpenGL ES ${MIN_OPENGL_VERSION} or later", Toast.LENGTH_LONG).show()
            //activity.finish()
            return false
        }
        return true
    }
}