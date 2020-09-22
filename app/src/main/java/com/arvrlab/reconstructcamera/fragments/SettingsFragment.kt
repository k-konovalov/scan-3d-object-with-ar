package com.arvrlab.reconstructcamera.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.arvrlab.reconstructcamera.R
import com.arvrlab.reconstructcamera.SingleViewModel
import kotlinx.android.synthetic.main.settings_fragment.*

class SettingsFragment(private val singleViewModel: SingleViewModel): DialogFragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = inflater.inflate(R.layout.settings_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnClickListeners()
        getPrefs()
    }

    private fun initOnClickListeners(){
        btnDialogSave.setOnClickListener {
            savePrefs()
            dismiss()
        }
    }

    private fun getPrefs(){
        val tripleOfEt by lazy {
            listOf(
                Triple(etFtpAddress, R.string.pref_ftp_address_key, R.string.pref_ftp_default),
                Triple(etFtpPort, R.string.pref_ftp_port_key, R.string.pref_ftp_port_default),
                Triple(etFtpUsername, R.string.pref_ftp_username_key, R.string.pref_ftp_default),
                Triple(etFtpPassword, R.string.pref_ftp_password_key, R.string.pref_ftp_default)
            )
        }
        singleViewModel.getPrefs(tripleOfEt)
    }

    private fun savePrefs(){
        val etArray = arrayOf(
            Pair(etFtpAddress, R.string.pref_ftp_address_key),
            Pair(etFtpPort, R.string.pref_ftp_port_key),
            Pair(etFtpUsername, R.string.pref_ftp_username_key),
            Pair(etFtpPassword, R.string.pref_ftp_password_key)
        )
        singleViewModel.savePrefs(etArray)
    }
}