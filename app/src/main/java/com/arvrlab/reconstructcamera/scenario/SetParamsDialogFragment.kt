package com.arvrlab.reconstructcamera.scenario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.arvrlab.reconstructcamera.R

data class Params(var shutter: Int = 0, var iso: Int = 0, var wb: Int = 0, var focus: Int = 0)

class SetParamsDialogFragment(private val shuttervalue: List<Double>, val onSubmitButtonClicked: (Params) -> Unit) : DialogFragment(){
    private  val iso: EditText by lazy { requireView().findViewById<EditText>(R.id.etIso) }
    private  val focus: EditText by lazy { requireView().findViewById<EditText>(R.id.etFocus) }
    private  val wb: EditText by lazy { requireView().findViewById<EditText>(R.id.etWB) }
    private  val shutter: Spinner by lazy { requireView().findViewById<Spinner>(R.id.spinnerShutter) }
    private var shutterMap = mapOf(
        0.0 to "Shutter",
        30.0 to "30",
        15.0 to "15",
        8.0 to "8",
        4.0 to "4",
        2.0 to "2",
        1.0 to "1",
        1.0 / 2 to "1/2",
        1.0 / 4 to "1/4",
        1.0 / 8 to "1/8",
        1.0 / 15 to "1/15",
        1.0 / 30 to "1/30",
        1.0 / 60 to "1/125",
        1.0 / 125 to "1/125",
        1.0 / 250 to "1/250",
        1.0 / 500 to "1/500",
        1.0 / 1000 to "1/1000",
        1.0 / 2000 to "1/2000",
        1.0 / 4000 to "1/4000",
        1.0 / 8000 to "1/8000"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_set_camera_params, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shutterKeys = mutableListOf<String>()
            shuttervalue.forEach { value ->
                 shutterMap[value]?.let { shutterKeys.add(it) }
            }

        ArrayAdapter<String>(requireContext(), R.layout.spinner_item, shutterKeys
        ).also { shutter.adapter = it }

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
        val params = Params(shutter.selectedItemPosition, iso.text.toString().toInt(), wb.text.toString().toInt(), focus.text.toString().toInt())
        onSubmitButtonClicked(params)
        dismiss()
    }

}
