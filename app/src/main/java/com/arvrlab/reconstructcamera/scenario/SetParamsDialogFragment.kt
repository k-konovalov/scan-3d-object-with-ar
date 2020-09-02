package com.arvrlab.reconstructcamera.scenario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import com.arvrlab.reconstructcamera.R

data class Params(var shutter: Double = 0.0, var iso: Int = 0, var wb: Int = 0, var focus: Int = 0)

class SetParamsDialogFragment(val onSubmitButtonClicked: (Params) -> Unit) : DialogFragment(),
    View.OnClickListener {

    private var shutterSpeeds = mapOf(
        "Shutter" to 0.0,
        "30" to 30.0,
        "15" to 15.0,
        "8" to 8.0,
        "4" to 4.0,
        "2" to 2.0,
        "1" to 1.0,
        "1/2" to 1.0 / 2,
        "1/4" to 1.0 / 4,
        "1/8" to 1.0 / 8,
        "1/15" to 1.0 / 15,
        "1/30" to 1.0 / 30,
        "1/125" to 1.0 / 60,
        "1/125" to 1.0 / 125,
        "1/250" to 1.0 / 250,
        "1/500" to 1.0 / 500,
        "1/1000" to 1.0 / 1000,
        "1/2000" to 1.0 / 2000,
        "1/4000" to 1.0 / 4000,
        "1/8000" to 1.0 / 8000
    )

    private  var iso: EditText? = null
    private  var focus: EditText? = null
    private  var wb: EditText? = null
    private  var shutter: Spinner? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_set_camera_params, container, false)
        iso = rootView.findViewById(R.id.etIso)
        focus = rootView.findViewById(R.id.etFocus)
        wb = rootView.findViewById(R.id.etWB)
        shutter = rootView.findViewById(R.id.spinnerShutter)
        ArrayAdapter<String>(
            rootView.context,
            R.layout.spinner_item,
            shutterSpeeds.keys.toList()
        ).also {
            shutter?.adapter = it
        }
        rootView.findViewById<Button>(R.id.btnSubmit).setOnClickListener(this)
        rootView.findViewById<Button>(R.id.btnCancel).setOnClickListener(this)
        return rootView
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnSubmit -> onSubmitButtonClicked()
            R.id.btnCancel -> onCancelClicked()
        }
    }

    private fun onSubmitButtonClicked() {
        val params = Params(shutterSpeeds[shutter?.selectedItem.toString()] ?: 0.0)
        if (!iso?.text.isNullOrEmpty() && iso?.text?.isDigitsOnly() == true)
            params.iso = iso?.text.toString().toInt()
        if (!focus?.text.isNullOrEmpty() && focus?.text?.isDigitsOnly() == true)
            params.focus = focus?.text.toString().toInt()
        if (!wb?.text.isNullOrEmpty() && wb?.text?.isDigitsOnly() == true)
            params.wb = wb?.text.toString().toInt()
        onSubmitButtonClicked(params)
        dismiss()
    }

    private fun onCancelClicked() {
        dismiss()
    }

}
