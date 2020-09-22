package com.arvrlab.reconstructcamera

import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.arvrlab.reconstructcamera.core.CustomCameraX
import com.arvrlab.reconstructcamera.core.FtpSettings
import com.arvrlab.reconstructcamera.core.SharedPrefHelper
import com.google.android.material.textfield.TextInputEditText
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.ftp.FTPUploadRequest
import java.text.SimpleDateFormat
import java.util.*

class SingleViewModel: ViewModel() {
    val cameraX = CustomCameraX()
    private var ftpSettings = FtpSettings()
    lateinit var sharedPrefHelper: SharedPrefHelper

    //Todo: new class?
    fun uploadFileToFtp(context: Context, pathAndName: Pair<String, String>){
        if (ftpSettings.url.isNotEmpty()){
            val localFilePath = pathAndName.first
            val fileName = pathAndName.second

            val dateFormat = "dd.MM.yyyy"
            val remotePath = SimpleDateFormat(dateFormat, Locale.US).format(System.currentTimeMillis())
            val remoteFilePath = "/$remotePath/$fileName"

            try {
                FTPUploadRequest(context, ftpSettings.url, ftpSettings.port) //"my.ftpserver.com", 21)
                    .setUsernameAndPassword(ftpSettings.user, ftpSettings.password) //("ftpuser", "testpassword")
                    .addFileToUpload(localFilePath, remoteFilePath) //("/absolute/path/to/file", "/remote/path")
                    .setNotificationConfig(UploadServiceConfig.notificationConfigFactory)
                    .setMaxRetries(4)
                    .startUpload()
            } catch (e: Exception) {
                Toast.makeText(context, "Error during upload, check network or ftp settings", Toast.LENGTH_SHORT).show()
                Log.e("AndroidUploadService", "Error during upload", e) }
        }
    }

    fun getPrefs(tripleOfEt: List<Triple<EditText, Int, Int>>){
        tripleOfEt.forEach { prefToEditText(it.first,it.second,it.third) }
    }

    private fun prefToEditText(targetEt: EditText, key: Int, defKey: Int){
        sharedPrefHelper.run {
            val value = getString(key, defKey).toCharArray()
            if (value.isNotEmpty()) targetEt.setText(value,0,value.size)
        }
    }

    fun savePrefs(pairOfEt: Array<Pair<TextInputEditText, Int>>) {
        pairOfEt.forEach { editTextToPref(it.first, it.second) }
        fillFtpSettings(pairOfEt)
    }

    private fun editTextToPref(targetEt: EditText, key: Int){
        sharedPrefHelper.putString(key, targetEt.text.toString())
    }

    private fun fillFtpSettings(pairOfEt: Array<Pair<TextInputEditText, Int>>) {
        try {
            ftpSettings = FtpSettings(
                url = pairOfEt[0].first.text.toString(),
                port = pairOfEt[1].first.text.toString().toInt(),
                user = pairOfEt[2].first.text.toString(),
                password = pairOfEt[3].first.text.toString()
            )
        } catch (e: Exception) { Log.e("Pref", "Error during set FTP settings", e) }
    }
}