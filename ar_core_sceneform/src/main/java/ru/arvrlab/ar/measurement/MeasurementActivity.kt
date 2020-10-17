package ru.arvrlab.ar.measurement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.AlertDialog
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import ru.arvrlab.ar.measurement.core.Constants
import kotlinx.android.synthetic.main.activity_measurement.*
import com.google.ar.sceneform.rendering.Color as arColor


class MeasurementActivity : AppCompatActivity(R.layout.activity_measurement) {
    override fun onStart() {
        super.onStart()
        requestPermissions(Constants.permissions, 1)
    }
}