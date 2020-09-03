package com.arvrlab.reconstructcamera.scenario

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.arvrlab.reconstructcamera.CustomCameraX
import com.arvrlab.reconstructcamera.R

class SetParamsDialogFragment(private val manualParameters: CustomCameraX.Parameters.ManualParameters) : DialogFragment(){
    private  val etIso: EditText by lazy { requireView().findViewById<EditText>(R.id.etIso) }
    private  val etFocus: EditText by lazy { requireView().findViewById<EditText>(R.id.etFocus) }
    private  val etWb: EditText by lazy { requireView().findViewById<EditText>(R.id.etWB) }
    private  val spinnerShutter: Spinner by lazy { requireView().findViewById<Spinner>(R.id.spinnerShutter) }
    private var shutterSpeeds = mapOf(
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_set_camera_params, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ArrayAdapter<String>(requireContext(), R.layout.spinner_item, shutterSpeeds.filter { it.value < 1.0 }.keys.toList()
        ).also { spinnerShutter.adapter = it }
        manualParameters.run {
            etIso.setText(iso.toString())
            etFocus.setText(focus.toString())
            etWb.setText(wb.toString())
            spinnerShutter.setSelection(shutterIndex)
        }


        view.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            onSubmitButtonClicked()
        }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun onSubmitButtonClicked() {
        manualParameters.apply {
            iso = etIso.text.toString().toInt()
            wb = etWb.text.toString().toInt()
            focus = etFocus.text.toString().toInt()
            shutterIndex = spinnerShutter.selectedItemPosition
        }
        dismiss()
    }

}
