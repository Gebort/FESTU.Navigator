package com.example.festunavigator.presentation

import android.annotation.SuppressLint
import android.net.Uri
import android.opengl.Matrix
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.festunavigator.R
import com.example.festunavigator.data.ml.classification.MLKitObjectDetector
import com.example.festunavigator.domain.ml.DetectedObjectResult
import com.example.festunavigator.domain.repository.Tree
import com.example.festunavigator.domain.repository.TreeNode
import com.example.festunavigator.presentation.common.helpers.DisplayRotationHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.collision.pickHitTests
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*


/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(val activity: MainActivity) : DefaultLifecycleObserver, CoroutineScope by MainScope() {
    companion object {
        val TAG = "HelloArRenderer"
        val mode = "ADMIN"
    }


    lateinit var view: MainActivityView

    var selectedNode: Node? = null
    val tree = Tree()
    val treeNodesToModels: MutableMap<TreeNode, ArModelNode> = mutableMapOf()
    val modelsToTreeNode: MutableMap<ArModelNode, TreeNode> = mutableMapOf()

    var linkPlacement = false

    val displayRotationHelper = DisplayRotationHelper(activity)
    //val pointCloudRender = PointCloudRender()

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)

    val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
    var scanButtonWasPressed = false


    val mlKitAnalyzer = MLKitObjectDetector(activity)

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
//            //scanButtonWasPressed = true
//        }

        selectNode(null)

        view.surfaceView.onTouchEvent = {pickHitResult, motionEvent ->

            pickHitResult.node?.let { node ->
                if (!linkPlacement) {
                    selectNode(node)
                }
                else {
                    linkNodes(selectedNode!!, node)
                }
            }
            true
        }

        view.delete.setOnClickListener {
            modelsToTreeNode[selectedNode]?.let {
                treeNodesToModels.remove(it)
                modelsToTreeNode.remove(selectedNode)
                tree.removeNode(it)
                selectedNode?.destroy()
                selectNode(null)
            }

        }

        view.link.setOnClickListener {
            linkPlacementMode(!linkPlacement)
        }

        view.place.setOnClickListener {
            createPath()
        }

        view.surfaceView.onFrame = { frameTime ->
            onDrawFrame(frameTime)

        }



//        view.scanButton.setOnClickListener {
//            // frame.acquireCameraImage is dependent on an ARCore Frame, which is only available in onDrawFrame.
//            // Use a boolean and check its state in onDrawFrame to interact with the camera image.
//            scanButtonWasPressed = true
//            view.setScanningActive(true)
//            hideSnackbar()
//        }



    }



    var objectResults: List<DetectedObjectResult>? = null

    private fun onDrawFrame(frameTime: FrameTime) {

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
                    arLabeledAnchors.find { it.anchor.pose == anchor.pose }
                if (existingAnchor == null) {
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

                // createRenderable(arDetectedObject.label)

            }

            view.post {
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

    fun linkPlacementMode(link: Boolean){
        linkPlacement = link
        view.link.text = if (link) "Cancel" else "Link"
    }

    fun selectNode(node: Node?){
        selectedNode = node
        view.link.isEnabled = node != null
        view.delete.isEnabled = node != null
    }

    fun linkNodes(node1: Node, node2: Node){
        val path1: TreeNode? = modelsToTreeNode[node1]
        val path2: TreeNode? = modelsToTreeNode[node2]

        if (path1 == null && path2 == null)
            return

        tree.addLink(path1!!, path2!!)
        drawLine(node1, node2)
    }

    fun createPath(){
        val modelNode = ArModelNode()
        modelNode.loadModelAsync(context = view.activity.applicationContext,
            coroutineScope = view.activity.lifecycleScope,
            glbFileLocation = "models/cylinder.glb",
        )
        modelNode.position = Position(0f, 0f, 0f)
        modelNode.modelScale = Scale(0.1f)
        modelNode.autoAnchor = true

        val pathTreeNode = TreeNode.Path(tree.size, modelNode.position)
        tree.addNode(pathTreeNode)
        treeNodesToModels[pathTreeNode] = modelNode
        modelsToTreeNode[modelNode] = pathTreeNode

        view.surfaceView.addChild(modelNode)

    }

//    fun drawTreeNode(node: TreeNode) {
//        ViewRenderable.builder()
//            .setView(view.activity, R.layout.text_sign)
//            .build()
//            .thenAccept { renderable: ViewRenderable ->
//                renderable.let {
//                    it.isShadowCaster = false
//                    it.isShadowReceiver = false
//                }
//                val textView = renderable.view as TextView?
//                textView?.let {
//                    it.text = if (node is TreeNode.Entry) "Entry ${node.number}" else "Id ${node.id}"
//                }
//                val textNode = ArNode().apply {
//                    setModel(
//                        renderable = renderable
//                    )
//                    position = Position(node.x, node.y, node.y)
//                }
//                view.surfaceView.addChild(textNode)
//            }
//    }

    fun drawLine(from: Node, to: Node){

        val fromVector = from.position.toVector3()
        val toVector = to.position.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = Color(android.graphics.Color.parseColor("#ffffff"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(view.activity.applicationContext, colorOrange)
            .thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    0.01f, lineLength,
                    Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                val node = ArNode()
                node.setModel(model)
                //node.parent = anchorNode
                view.surfaceView.addChild(node)

                // 4. set rotation
                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB: Quaternion =
                    Quaternion.lookRotation(
                        directionFromTopToBottom,
                        Vector3.up()
                    )

                val rotation = Quaternion.multiply(
                    rotationFromAToB,
                    Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                )

                node.modelQuaternion = dev.romainguy.kotlin.math.Quaternion(
                    rotation.x,
                    rotation.y,
                    rotation.z,
                    rotation.w
                )
                node.position = from.position
            }
    }

//    fun drawAllNodes(tree: Tree){
//        tree.allPoints.values.forEach { node ->
//           drawTreeNode(node)
//        }
//        tree.links.keys.forEach { node1 ->
//            tree.links[node1]!!.forEach { node2 ->
//                drawLine(node1, node2)
//            }
//        }
//    }

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