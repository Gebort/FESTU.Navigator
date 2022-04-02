package com.example.festunavigator

import android.annotation.SuppressLint
import android.opengl.Matrix
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.festunavigator.common.helpers.DisplayRotationHelper
import com.example.festunavigator.common.samplerender.arcore.BackgroundRenderer
import com.example.festunavigator.ml.classification.DetectedObjectResult
import com.example.festunavigator.ml.classification.MLKitObjectDetector
import com.example.festunavigator.ml.render.LabelRender
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.localPosition
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import io.github.sceneview.model.GLBLoader.loadModelAsync
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.function.Consumer


/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(val activity: MainActivity) : DefaultLifecycleObserver, CoroutineScope by MainScope() {
    companion object {
        val TAG = "HelloArRenderer"
    }

    //var anchor: Anchor? = null

    lateinit var view: MainActivityView

    val displayRotationHelper = DisplayRotationHelper(activity)
    //val pointCloudRender = PointCloudRender()

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)

    val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
    var scanButtonWasPressed = false


    val mlKitAnalyzer = MLKitObjectDetector(activity)
    //val gcpAnalyzer = GoogleCloudVisionDetector(activity)

    var currentAnalyzer: MLKitObjectDetector = mlKitAnalyzer

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    fun bindView(view: MainActivityView) {
        this.view = view

//        view.surfaceView.onTouchAr = { _, _ ->
//            scanButtonWasPressed = true
//        }

        view.surfaceView.onFrame = { frameTime ->
            onDrawFrame(frameTime)

        }

        view.place.setOnClickListener {
            createRenderable(
                view.x_edit.text.toString().toFloat(),
                view.y_edit.text.toString().toFloat(),
                view.z_edit.text.toString().toFloat()
            )
        }

//        view.scanButton.setOnClickListener {
//            // frame.acquireCameraImage is dependent on an ARCore Frame, which is only available in onDrawFrame.
//            // Use a boolean and check its state in onDrawFrame to interact with the camera image.
//            scanButtonWasPressed = true
//            view.setScanningActive(true)
//            hideSnackbar()
//        }

//        view.useCloudMlSwitch.setOnCheckedChangeListener { _, isChecked ->
//            currentAnalyzer = if (isChecked) gcpAnalyzer else mlKitAnalyzer
//        }


//        val gcpConfigured = gcpAnalyzer.credentials != null
//        view.useCloudMlSwitch.isChecked = gcpConfigured
//        view.useCloudMlSwitch.isEnabled = gcpConfigured
//        currentAnalyzer = if (gcpConfigured) gcpAnalyzer else mlKitAnalyzer
//
//        if (!gcpConfigured) {
//            showSnackbar("Google Cloud Vision isn't configured (see README). The Cloud ML switch will be disabled.")
//        }

//        view.resetButton.setOnClickListener {
//            arLabeledAnchors.clear()
//            view.resetButton.isEnabled = false
//            hideSnackbar()
//        }
    }

//    override fun onSurfaceCreated(render: SampleRender?) {
//        backgroundRenderer = BackgroundRenderer(render).apply {
//            setUseDepthVisualization(render, false)
//        }
//        if (render != null) {
//            labelRenderer.onSurfaceCreated(render)
//        }
//    }

//    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
//        displayRotationHelper.onSurfaceChanged(width, height)
//    }

    var objectResults: List<DetectedObjectResult>? = null

    private fun onDrawFrame(frameTime: FrameTime) {


//        backgroundRenderer.updateDisplayGeometry(frame)
//        if (render != null) {
//            backgroundRenderer.drawBackground(render)
//        }

        val session = view.surfaceView.arSession ?: return
        val frame = session.currentFrame?.frame ?: return

        // Get camera and projection matrices.
        val camera = frame.camera
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Handle tracking failures.
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

//        // Draw point cloud.
//        frame.acquirePointCloud().use { pointCloud ->
//            pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
//        }

        // Frame.acquireCameraImage must be used on the GL thread.
        // Check if the button was pressed last frame to start processing the camera image.
        if (scanButtonWasPressed) {
            scanButtonWasPressed = false
            val cameraImage = frame.tryAcquireCameraImage()
            if (cameraImage != null) {
                // Call our ML model on an IO thread.
                launch(Dispatchers.IO) {
                    val cameraId = session.cameraConfig.cameraId
                    val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
                    objectResults = currentAnalyzer.analyze(cameraImage, imageRotation)
                    cameraImage.close()
                }
            }
        }

        /** If results were completed this frame, create [Anchor]s from model results. */
        val objects = objectResults
        if (objects != null) {
            objectResults = null
            Log.i(TAG, "$currentAnalyzer got objects: $objects")
//            val anchors = objects.mapNotNull { obj ->
//                val (atX, atY) = obj.centerCoordinate
//                val anchor = createAnchor(atX.toFloat(), atY.toFloat(), frame) ?: return@mapNotNull null
//                Log.i(TAG, "Created anchor ${anchor.pose} from hit test")
//                ARLabeledAnchor(anchor, obj.label)
//            }
            //arLabeledAnchors.addAll(anchors)

            val anchors = mutableListOf<ARLabeledAnchor>()
            for (obj in objects) {
                val (atX, atY) = obj.centerCoordinate
                val anchor =
                    createAnchor(atX.toFloat(), atY.toFloat(), frame) ?: continue
                val existingAnchor =
                    arLabeledAnchors.find{ it.anchor.pose == anchor.pose}
                if (existingAnchor == null){
                    Log.i(TAG, "Created anchor ${anchor.pose} from hit test")
                    val arLabeledAnchor = ARLabeledAnchor(anchor, obj.label)
                    anchors.add(arLabeledAnchor)
                }
            }

            arLabeledAnchors.addAll(anchors)

            // Draw labels at their anchor position.
            for (arDetectedObject in arLabeledAnchors) {
                val anchor = arDetectedObject.anchor
                if (anchor.trackingState != TrackingState.TRACKING) continue

                ViewRenderable.builder()
                    .setView(view.activity, R.layout.text_sign)
                    .build()
                    .thenAccept { renderable: ViewRenderable ->
                        renderable.isShadowCaster = false
                        renderable.isShadowReceiver = false
                        val textView = renderable.view as TextView?
                        textView?.text = arDetectedObject.label
                        val textNode = ArNode(anchor)
                        textNode.setModel(
                            renderable = renderable
                        )
                        textNode.modelPosition = Position(3f, 2f, 0f)
                        view.surfaceView.addChild(textNode)
                    }

            }

            view.post {
                //view.resetButton.isEnabled = arLabeledAnchors.isNotEmpty()
                //view.setScanningActive(false)
                when {
                    objects.isEmpty() && currentAnalyzer == mlKitAnalyzer && !mlKitAnalyzer.hasCustomModel() ->
                        showSnackbar("Default ML Kit classification model returned no results. " +
                                "For better classification performance, see the README to configure a custom model.")
                    objects.isEmpty() ->
                        showSnackbar("Classification model returned no results.")
                    anchors.size != objects.size ->
                        showSnackbar("Objects were classified, but could not be attached to an anchor. " +
                                "Try moving your device around to obtain a better understanding of the environment.")
                }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    fun createRenderable(x: Float, y: Float, z: Float) {
        ViewRenderable.builder()
            .setView(view.activity, R.layout.text_sign)
            .build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                val textView = renderable.view as TextView?
                textView?.text = "x: $x y: $y z: $z"
                val textNode = ArNode()
                textNode.setModel(
                    renderable = renderable
                )
                textNode.localPosition = Position(x, y, z)
                view.surfaceView.addChild(textNode)
            }
    }

    /**
     * Utility method for [Frame.acquireCameraImage] that maps [NotYetAvailableException] to `null`.
     */
    fun Frame.tryAcquireCameraImage() = try {
        acquireCameraImage()
    } catch (e: NotYetAvailableException) {
        null
    } catch (e: Throwable) {
        throw e
    }

        private fun showSnackbar(message: String): Unit =
            activity.view.snackbarHelper.showError(activity, message)

        private fun hideSnackbar() = activity.view.snackbarHelper.hide(activity)

    /**
     * Temporary arrays to prevent allocations in [createAnchor].
     */
    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)

    /** Create an anchor using (x, y) coordinates in the [Coordinates2d.IMAGE_PIXELS] coordinate space. */
    fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Anchor? {
        // IMAGE_PIXELS -> VIEW
        convertFloats[0] = xImage
        convertFloats[1] = yImage
        frame.transformCoordinates2d(
            Coordinates2d.IMAGE_PIXELS,
            convertFloats,
            Coordinates2d.VIEW,
            convertFloatsOut
        )

        // Conduct a hit test using the VIEW coordinates
        val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
        val result = hits.getOrNull(0) ?: return null
        return result.trackable.createAnchor(result.hitPose)
    }
}



data class ARLabeledAnchor(val anchor: Anchor, val label: String)