package com.example.android.ar

import android.content.DialogInterface
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.android.ar.helpers.*
import com.example.android.ar.samplerender.*
import com.example.android.ar.samplerender.arcore.BackgroundRenderer
import com.example.android.ar.samplerender.arcore.PlaneRenderer
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.exceptions.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
class MainActivity : AppCompatActivity(), SampleRender.Renderer {

    private var installRequested = false
    private var hasSetTextureNames = false

    //(https://ru.wikipedia.org/wiki/UV-преобразование)
    private var calculateUVTransform = true

    /** Manages AR system state and handles the session lifecycle.
     *  This class is the main entry point to the ARCore API.
     *  This class allows the user to create a session, configure it,
     *  start or stop it and, most importantly, receive frames that allow access
     *  to camera image and device pose.
     *  (https://developers.google.com/ar/reference/java/com/google/ar/core/Session?hl=ur)
     */
    private var session: Session? = null

    //Helpers
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private val displayRotationHelper: DisplayRotationHelper? by lazy { DisplayRotationHelper(this) }
    private val trackingStateHelper: TrackingStateHelper by lazy { TrackingStateHelper(this) }

    // Set up touch listener.
    private val tapHelper: TapHelper by lazy { TapHelper(this) }
    private val depthSettings: DepthSettings = DepthSettings()
    private val instantPlacementSettings: InstantPlacementSettings = InstantPlacementSettings()

    private var render: SampleRender? = null
    private var planeRenderer: PlaneRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null

    private var depthTexture: Texture? = null

    private val depthSettingsMenuDialogCheckboxes = BooleanArray(2)
    private val instantPlacementSettingsMenuDialogCheckboxes = BooleanArray(1)
    private var pointCloudVertexBuffer: VertexBuffer? = null
    private var pointCloudMesh: Mesh? = null
    private var pointCloudShader: Shader? = null

    // Keep track of the last point cloud rendered to avoid updating
    // the VBO (это расширение OpenGL (ARB_vertex_buffer_object), позволяющее работать с видеопамятью в части чтения/записи/рендеринга массивов вертексов (вершин) и индексов
    // (https://www.gamedev.ru/terms/vbo))
    // if point cloud was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private var lastPointCloudTimestamp: Long = 0

    //полигонная сетка
    private var virtualObjectMesh: Mesh? = null
    private var virtualObjectShader: Shader? = null
    private var virtualObjectDepthShader: Shader? = null

    // Anchors created from taps used for object placing with a given color.
    private class ColoredAnchor(val anchor: Anchor, var color: FloatArray, val trackable: Trackable)

    private val anchors = ArrayList<ColoredAnchor>()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16) // view x model
    private val modelViewProjectionMatrix = FloatArray(16) // projection x view x model
    private val viewLightDirection = FloatArray(4) // view x LIGHT_DIRECTION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView.setOnTouchListener(tapHelper)

        // Set up renderer.
        render = SampleRender(surfaceView, this, assets)

        installRequested = false
        calculateUVTransform = true

        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)

        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // false, if wasn't created because of exception
        if (!createSession()) return

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession()
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.")
            session = null
            return
        }

        surfaceView?.onResume()
        displayRotationHelper?.onResume()
    }

    private fun createSession(): Boolean {
        if (session == null) {
            try {
                // Check whether Google Play Services for AR is installed
                if (ArCoreApk.getInstance().requestInstall(this, !installRequested) == InstallStatus.INSTALL_REQUESTED) {
                    installRequested = true
                    return false
                }
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this)
                    return false
                }

                // Create the session.
                session = Session(this)
            } catch (e: Throwable) {
                val message = when (e) {
                    is UnavailableArcoreNotInstalledException -> "Please install ARCore"
                    is UnavailableUserDeclinedInstallationException -> "Please install ARCore"
                    is UnavailableApkTooOldException -> "Please update ARCore"
                    is UnavailableSdkTooOldException -> "Please update this app"
                    is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                    else -> "Failed to create AR session"
                }

                messageSnackbarHelper.showError(this, message)
                Log.e(TAG, "Exception creating session", e)
                return false
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        session?.run {
            displayRotationHelper?.onPause()
            surfaceView?.onPause()
            session?.pause()
        }
    }

    override fun onDestroy() {
        // Explicitly close ARCore Session to release native resources.
        // Review the API reference for important considerations before calling close() in apps with
        // more complicated lifecycle requirements:
        // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
        session?.run {
            session?.close()
            session = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }


    private fun initClickListeners() {
        btnSettings.setOnClickListener { v ->
            PopupMenu(this, v).apply {

                setOnMenuItemClickListener { item: MenuItem ->
                    settingsMenuClick(item)
                }
                inflate(R.menu.settings_menu)

            }.show()
        }
    }

    /** Menu button to launch feature specific settings.  */
    private fun settingsMenuClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.depth_settings) {
            launchDepthSettingsMenuDialog()
            return true
        } else if (item.itemId == R.id.instant_placement_settings) {
            launchInstantPlacementSettingsMenuDialog()
            return true
        }
        return false
    }

    override fun onSurfaceCreated(render: SampleRender) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.
        try {
            depthTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE)
            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render, depthTexture)

            // Point cloud
            pointCloudShader = Shader.createFromAssets(render,
                POINT_CLOUD_VERTEX_SHADER_NAME,
                POINT_CLOUD_FRAGMENT_SHADER_NAME,
                null
            ).set4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
                .set1("u_PointSize", 5.0f)

            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer = VertexBuffer(render, 4,null)
            pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS, null, arrayOf(pointCloudVertexBuffer))

            // Virtual object to render (Andy the android)
            val virtualObjectTexture: Texture = Texture.createFromAsset(render, "models/andy.png", Texture.WrapMode.CLAMP_TO_EDGE)
            virtualObjectMesh = Mesh.createFromAsset(render, "models/andy.obj")
            virtualObjectShader = createVirtualObjectShader(render, virtualObjectTexture,false)
            virtualObjectDepthShader = createVirtualObjectShader(render, virtualObjectTexture, true)
                .setTexture("u_DepthTexture", depthTexture)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper?.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        if (session == null) {
            return
        }

        if (!hasSetTextureNames) {
            // Sets the OpenGL texture names (ids) that will be assigned to incoming camera frames in sequence in a ring buffer.
            // The textures must be bound to the GL_TEXTURE_EXTERNAL_OES target for use.
            // Shaders accessing these textures must use a samplerExternalOES sampler.
            //Passing multiple textures allows for a multithreaded rendering pipeline, unlike setCameraTextureName(int).
            session?.setCameraTextureNames(backgroundRenderer?.textureId?.let { intArrayOf(it) })
            hasSetTextureNames = true
        }

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper?.updateSessionIfNeeded(session)

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
           // Updates the state of the ARCore system. This includes: receiving a new camera frame,
            // updating the location of the device, updating the location of tracking anchors, updating detected planes, etc.
           // (https://developers.google.com/ar/reference/java/com/google/ar/core/Session#public-frame-update)

            //Frame Фиксирует состояние и изменения в системе AR от вызова update()
            val frame = session?.update() ?: return
            val camera = frame.camera

            // The UV Transform represents the transformation between screen space in normalized units
            // and screen space in units of pixels.  Having the size of each pixel is necessary in the
            // virtual object shader, to perform kernel-based blur effects.
            if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
                calculateUVTransform = false
                val transform = getTextureTransformMatrix(frame)
                virtualObjectDepthShader?.setMatrix3("u_DepthUvTransform", transform)
            }
            if (session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
                // The rendering abstraction leaks a bit here. Populate the depth texture with the current
                // frame data.
                try {
                    frame.acquireDepthImage().let { depthImage ->
                        depthTexture?.textureId?.let { GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, it)
                        }

                        GLES30.glTexImage2D(
                            GLES30.GL_TEXTURE_2D,
                            0,
                            GLES30.GL_RG8,
                            depthImage.width,
                            depthImage.height,
                            0,
                            GLES30.GL_RG,
                            GLES30.GL_UNSIGNED_BYTE,
                            depthImage.planes[0].buffer
                        )

                        val aspectRatio = depthImage.width.toFloat() / depthImage.height.toFloat()
                        virtualObjectDepthShader?.set1("u_DepthAspectRatio", aspectRatio)
                    }
                } catch (e: NotYetAvailableException) {
                    // This normally means that depth data is not available yet. This is normal so we will not
                    // spam the logcat with this.
                }
            }

            // Handle one tap per frame.
            handleTap(frame, camera)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer?.draw(render, frame, depthSettings.depthColorVisualizationEnabled())

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.trackingState == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(this, TrackingStateHelper.getTrackingFailureReasonString(camera))
                return
            }

            // Get projection matrix.
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            camera.getViewMatrix(viewMatrix, 0)

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)
            frame.acquirePointCloud().use { pointCloud ->
                if (pointCloud.timestamp > lastPointCloudTimestamp) {
                    pointCloudVertexBuffer?.set(pointCloud.points)
                    lastPointCloudTimestamp = pointCloud.timestamp
                }

                Matrix.multiplyMM(
                    modelViewProjectionMatrix,
                    0,
                    projectionMatrix,
                    0,
                    viewMatrix,
                    0
                )

                pointCloudShader?.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
                render.draw(pointCloudMesh, pointCloudShader)
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                messageSnackbarHelper.hide(this)
            } else {
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE)
            }

            // Visualize planes.
            planeRenderer?.drawPlanes(
                render,
                session?.getAllTrackables(Plane::class.java),
                camera.displayOrientedPose,
                projectionMatrix
            )

            // Visualize anchors created by touch.
             anchors.forEach { coloredAnchor ->
                 if (coloredAnchor.anchor.trackingState == TrackingState.TRACKING) {

                     // For anchors attached to Instant Placement points, update the color once the tracking
                     // method becomes FULL_TRACKING.
                     val isFullTracking = coloredAnchor.trackable is InstantPlacementPoint
                             && coloredAnchor.trackable.trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING
                     if (isFullTracking) {
                         coloredAnchor.color = getTrackableColor(coloredAnchor.trackable)
                     }

                     // Get the current pose of an Anchor in world space. The Anchor pose is updated
                     // during calls to session.update() as ARCore refines its estimate of the world.
                     coloredAnchor.anchor.pose.toMatrix(modelMatrix, 0)

                     // Calculate model/view/projection matrices and view-space light direction
                     Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
                     Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
                     Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, LIGHT_DIRECTION, 0)

                     // Update shader properties and draw
                     val shader: Shader? =
                         if (depthSettings.useDepthForOcclusion()) virtualObjectDepthShader else virtualObjectShader
                     shader
                         ?.setMatrix4("u_ModelView", modelViewMatrix)
                         ?.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
                         ?.set4("u_ColorCorrection", colorCorrectionRgba)
                         ?.set4("u_ViewLightDirection", viewLightDirection)
                         ?.set3("u_AlbedoColor", coloredAnchor.color)
                     render.draw(virtualObjectMesh, shader)
                 }
             }
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        val tap: MotionEvent = tapHelper.poll() ?: return
        if (camera.trackingState == TrackingState.TRACKING) {
            val hitResultList: List<HitResult> =
                if (instantPlacementSettings.isInstantPlacementEnabled) {
                    frame.hitTestInstantPlacement(tap.x, tap.y, APPROXIMATE_DISTANCE_METERS)
                } else {
                    frame.hitTest(tap)
                }
            hitResultList.forEach { hit ->
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                val trackable = hit.trackable
                // If a plane was hit, check that it was hit inside the plane polygon.
                val wasHitInsideThePlanePolygon = (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0)
                        || (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        || trackable is InstantPlacementPoint
                if (wasHitInsideThePlanePolygon) {
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size >= 20) {
                        anchors[0].anchor.detach()
                        anchors.removeAt(0)
                    }

                    val objColor = getTrackableColor(trackable)
                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(ColoredAnchor(hit.createAnchor(), objColor, trackable))
                    // For devices that support the Depth API, shows a dialog to suggest enabling
                    // depth-based occlusion. This dialog needs to be spawned on the UI thread.
                    runOnUiThread { showOcclusionDialogIfNeeded() }

                    // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
                    // Instant Placement Point.
                    return@handleTap
                }
            }
        }
    }

    /**
     * Shows a pop-up dialog on the first call, determining whether the user wants to enable
     * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
     */
    private fun showOcclusionDialogIfNeeded() {
        val isDepthSupported = session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) ?: false
        if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return  // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(
                R.string.button_text_enable_depth
            ) { _: DialogInterface?, _: Int -> depthSettings.setUseDepthForOcclusion(true) }
            .setNegativeButton(
                R.string.button_text_disable_depth
            ) { _: DialogInterface?, _: Int -> depthSettings.setUseDepthForOcclusion(false) }
            .show()
    }

    private fun launchInstantPlacementSettingsMenuDialog() {
        resetSettingsMenuDialogCheckboxes()
        val resources = resources
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_instant_placement)
            .setMultiChoiceItems(resources.getStringArray(R.array.instant_placement_options_array), instantPlacementSettingsMenuDialogCheckboxes)
            { _: DialogInterface?, which: Int, isChecked: Boolean ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked
            }
            .setPositiveButton(R.string.done) { _: DialogInterface?, _: Int ->
                applySettingsMenuDialogCheckboxes()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                resetSettingsMenuDialogCheckboxes()
            }
            .show()
    }

    /** Shows checkboxes to the user to facilitate toggling of depth-based effects.  */
    private fun launchDepthSettingsMenuDialog() {
        // Retrieves the current settings to show in the checkboxes.
        resetSettingsMenuDialogCheckboxes()

        // Shows the dialog to the user.
        val isDepthSupported = session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) ?: false
        if (isDepthSupported) {
            // With depth support, the user can select visualization options.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_with_depth)
                .setMultiChoiceItems(
                    resources.getStringArray(R.array.depth_options_array),
                    depthSettingsMenuDialogCheckboxes
                ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    depthSettingsMenuDialogCheckboxes[which] = isChecked
                }
                .setPositiveButton(R.string.done) { _: DialogInterface?, _: Int ->
                    applySettingsMenuDialogCheckboxes()
                }
                .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                    resetSettingsMenuDialogCheckboxes()
                }
                .show()
        } else {
            // Without depth support, no settings are available.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_without_depth)
                .setPositiveButton(
                    R.string.done
                ) { _: DialogInterface?, _: Int -> applySettingsMenuDialogCheckboxes() }
                .show()
        }
    }

    private fun applySettingsMenuDialogCheckboxes() {
        depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0])
        depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1])
        instantPlacementSettings.isInstantPlacementEnabled =
            instantPlacementSettingsMenuDialogCheckboxes[0]
        configureSession()
    }

    private fun resetSettingsMenuDialogCheckboxes() {
        depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion()
        depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled()
        instantPlacementSettingsMenuDialogCheckboxes[0] =
            instantPlacementSettings.isInstantPlacementEnabled
    }

    /** Checks if we detected at least one plane.  */
    private fun hasTrackingPlane(): Boolean {
       session?.getAllTrackables(Plane::class.java)?.forEach { plane ->
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    /** Configures the session with feature settings.  */
    private fun configureSession() {
        session?.let {
            val config = it.config
            if (it.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            } else {
                config.depthMode = Config.DepthMode.DISABLED
            }

            if (instantPlacementSettings.isInstantPlacementEnabled) {
                config.instantPlacementMode = InstantPlacementMode.LOCAL_Y_UP
            } else {
                config.instantPlacementMode = InstantPlacementMode.DISABLED
            }
            it.configure(config)
        }
    }

    /**
     * Assign a color to the object for rendering based on the trackable type this anchor attached to.
     * For AR_TRACKABLE_POINT, it's blue color.
     * For AR_TRACKABLE_PLANE, it's green color.
     * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT while tracking method is
     * SCREENSPACE_WITH_APPROXIMATE_DISTANCE, it's white color.
     * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT once tracking method becomes FULL_TRACKING, it's
     * orange color.
     * The color will update for an InstantPlacementPoint once it updates its tracking method from
     * SCREENSPACE_WITH_APPROXIMATE_DISTANCE to FULL_TRACKING.
     */
    private fun getTrackableColor(trackable: Trackable): FloatArray {
        var trackableColor = floatArrayOf(0f, 0f, 0f)
        when (trackable) {
            is Point -> {
                trackableColor = floatArrayOf(66.0f / 255.0f, 133.0f / 255.0f, 244.0f / 255.0f)
            }
            is Plane -> {
                trackableColor = floatArrayOf(139.0f / 255.0f, 195.0f / 255.0f, 74.0f / 255.0f)
            }
            is InstantPlacementPoint -> {
                if (trackable.trackingMethod == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
                    trackableColor = floatArrayOf(255.0f / 255.0f, 255.0f / 255.0f, 255.0f / 255.0f)
                }
                if (trackable.trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING) {
                    trackableColor = floatArrayOf(255.0f / 255.0f, 167.0f / 255.0f, 38.0f / 255.0f)
                }
            }
        }
        // Fallback color.
        return trackableColor
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val SEARCHING_PLANE_MESSAGE = "Searching for surfaces..."

        // Assumed distance from the device camera to the surface on which user will try to place objects.
        // This value affects the apparent scale of objects while the tracking method of the the
        // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
        // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
        // values for AR experiences where users are expected to place objects on surfaces close to the
        // camera. Use larger values for experiences where the user will likely be standing and trying to
        // place an object on the ground or floor in front of them.
        private const val APPROXIMATE_DISTANCE_METERS = 2.0f

        // Point Cloud
        private const val POINT_CLOUD_VERTEX_SHADER_NAME = "shaders/point_cloud.vert"
        private const val POINT_CLOUD_FRAGMENT_SHADER_NAME = "shaders/point_cloud.frag"

        // Virtual object
        private const val AMBIENT_INTENSITY_VERTEX_SHADER_NAME = "shaders/ambient_intensity.vert"
        private const val AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME = "shaders/ambient_intensity.frag"

        // Note: the last component must be zero to avoid applying the translational part of the matrix.
        private val LIGHT_DIRECTION = floatArrayOf(0.250f, 0.866f, 0.433f, 0.0f)

        /**
         * Returns a transformation matrix that when applied to screen space uvs makes them match
         * correctly with the quad texture coords used to render the camera feed. It takes into account
         * device orientation.
         */
        private fun getTextureTransformMatrix(frame: Frame): FloatArray {
            val frameTransform = FloatArray(6)
            val uvTransform = FloatArray(9)
            // XY pairs of coordinates in NDC space that constitute the origin and points along the two
            // principal axes.
            val ndcBasis = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

            // Temporarily store the transformed points into outputTransform.
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcBasis,
                Coordinates2d.TEXTURE_NORMALIZED,
                frameTransform
            )

            // Convert the transformed points into an affine transform and transpose it.
            val ndcOriginX = frameTransform[0]
            val ndcOriginY = frameTransform[1]
            uvTransform[0] = frameTransform[2] - ndcOriginX
            uvTransform[1] = frameTransform[3] - ndcOriginY
            uvTransform[2] = 0F
            uvTransform[3] = frameTransform[4] - ndcOriginX
            uvTransform[4] = frameTransform[5] - ndcOriginY
            uvTransform[5] = 0F
            uvTransform[6] = ndcOriginX
            uvTransform[7] = ndcOriginY
            uvTransform[8] = 1F
            return uvTransform
        }

        private fun createVirtualObjectShader(render: SampleRender, virtualObjectTexture: Texture, useDepthForOcclusion: Boolean): Shader {
            return Shader.createFromAssets(
                render,
                AMBIENT_INTENSITY_VERTEX_SHADER_NAME,
                AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME,
                hashMapOf<String, String>("USE_DEPTH_FOR_OCCLUSION" to if (useDepthForOcclusion) "1" else "0")
            )
                .setBlend(Shader.BlendFactor.SRC_ALPHA, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA)
                .setTexture("u_AlbedoTexture", virtualObjectTexture)
                .set1("u_UpperDiffuseIntensity", 1.0f)
                .set1("u_LowerDiffuseIntensity", 0.5f)
                .set1("u_SpecularIntensity", 0.2f)
                .set1("u_SpecularPower", 8.0f)
        }
    }
}
