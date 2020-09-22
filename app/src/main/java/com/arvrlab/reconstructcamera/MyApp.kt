package com.arvrlab.reconstructcamera

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import net.gotev.uploadservice.UploadServiceConfig

const val CHANNEL_ID = "com.arvrlab.reconstructcamera.ftp"
class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        UploadServiceConfig.initialize(this, CHANNEL_ID, true)
    }

    private fun createNotificationChannel(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "FTP Upload",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Show upload progress"
            //channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }
    }
}