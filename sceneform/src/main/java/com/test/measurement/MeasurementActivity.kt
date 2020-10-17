package com.test.measurement

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.os.*
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.activity_measurement.*
import java.util.*

class MeasurementActivity : AppCompatActivity(R.layout.activity_measurement) {
