package com.example.festunavigator.presentation

import android.annotation.SuppressLint
import android.opengl.Matrix
import android.opengl.Visibility
import android.util.Log
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.festunavigator.R
import com.example.festunavigator.data.ml.classification.MLKitObjectDetector
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.ml.DetectedObjectResult
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.tree.WrongEntryException
import com.example.festunavigator.domain.use_cases.*
import com.example.festunavigator.presentation.common.helpers.DisplayRotationHelper
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.*
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.*
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.TAG
import kotlinx.coroutines.*
import java.util.*


/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(
    val activity: MainActivity,
    val deleteNodes: DeleteNodes,
    val getTree: GetTree,
    val insertNodes: InsertNodes,
    val updateNodes: UpdateNodes,
    val findWay: FindWay,
    val hitTest: HitTest
    ) : DefaultLifecycleObserver, CoroutineScope by MainScope() {

    companion object {
        val TAG = "HelloArRenderer"
        val mode = "ADMIN"
    }

    lateinit var view: MainActivityView

    var selectedNode: Node? = null
    var tree = Tree()
    val treeNodesToModels: MutableBiMap<TreeNode, Node> = mutableBiMapOf()
    val modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()
    val linksToWayModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()

    var linkPlacement = false

    val displayRotationHelper = DisplayRotationHelper(activity)
    //val pointCloudRender = PointCloudRender()

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)

   // val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
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
//
//        }

        selectNode(null)

        preload()

        view.surfaceView.onTouchEvent = {pickHitResult, motionEvent ->

            pickHitResult.node?.let { node ->
                if (!linkPlacement) {
                    selectNode(node)

                }
                else {
                    linkNodes(selectedNode!!, node)
                    linkPlacementMode(false)
                }
            }
            true
        }

        view.delete.setOnClickListener {
            removeNode(selectedNode)
        }

        view.link.setOnClickListener {
            linkPlacementMode(!linkPlacement)
        }

        view.place.setOnClickListener {
            createPath()
        }

        view.entry.setOnClickListener {
            createEntryByDialog()

        }

        view.surfaceView.onFrame = { frameTime ->
            onDrawFrame(frameTime)

        }

        view.init.setOnClickListener {
            initializeByDialog()
        }

        view.pathfind.setOnClickListener {
            if (tree.initialized) {
                pathfindByDialog()
            }
            else {
                showSnackbar("Tree isnt init")
            }
        }

        view.rend.setOnClickListener {
            rendTest()
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

//            val anchors = mutableListOf<ARLabeledAnchor>()
//            for (obj in objects) {
//                val (atX, atY) = obj.centerCoordinate
//                val anchor =
//                    createAnchor(atX.toFloat(), atY.toFloat(), frame) ?: continue
//                val existingAnchor =
//                    arLabeledAnchors.find { it.anchor.pose == anchor.pose }
//                if (existingAnchor == null) {
//                    Log.i(TAG, "Created anchor ${anchor.pose} from hit test")
//                    val arLabeledAnchor =
//                        com.example.festunavigator.presentation.ARLabeledAnchor(anchor, obj.label)
//                    anchors.add(arLabeledAnchor)
//                }
//            }

//            arLabeledAnchors.addAll(anchors)

//            // Draw labels at their anchor position.
//            for (arDetectedObject in arLabeledAnchors) {
//                val anchor = arDetectedObject.anchor
//                if (anchor.trackingState != TrackingState.TRACKING) continue
//
//                // createRenderable(arDetectedObject.label)
//
//            }

//            view.post {
//                when {
//                    objects.isEmpty() && currentAnalyzer == mlKitAnalyzer && !mlKitAnalyzer.hasCustomModel() ->
//                        showSnackbar("Default ML Kit classification model returned no results. " +
//                                "For better classification performance, see the README to configure a custom model.")
//                    objects.isEmpty() ->
//                        showSnackbar("Classification model returned no results.")
//                    anchors.size != objects.size ->
//                        showSnackbar("Objects were classified, but could not be attached to an anchor. " +
//                                "Try moving your device around to obtain a better understanding of the environment.")
//                }
//            }
        }

    }

    fun rendTest(){
        view.activity.lifecycleScope.launch {
            val result = hitTest(view.surfaceView)
            val position = result.getOrNull()
            if (position != null){
                placeRend(position)
            }
            else {
                showSnackbar("Null pos")
            }
        }
    }

    fun placeRend(pos: OrientatedPosition){
        ViewRenderable.builder()
            .setView(view.activity, R.layout.text_sign)
            .build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.let {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                }
                val cardView = renderable.view as CardView?
                cardView?.let {
//                    val textView: TextView = cardView.findViewById(R.id.signTextView)
//                    textView.text =
//                        if (node is TreeNode.Entry) "Entry ${node.number}" else "Id ${node.id}"
                    val textNode = ArNode().apply {
                        setModel(
                            renderable = renderable
                        )
                        position = Position(pos.position.x, pos.position.y, pos.position.z)
                        quaternion = pos.orientation

                        anchor = this.createAnchor()
                    }
                    view.surfaceView.addChild(textNode)
                }
            }
    }

    fun linkPlacementMode(link: Boolean){
        linkPlacement = link
        view.link.text = if (link) "Cancel" else "Link"
    }

    fun removeNode(node: Node?){
        modelsToLinkModels.keys
            .filter { pair ->
                pair.first == node || pair.second == node
            }
            .forEach { pair ->
                modelsToLinkModels[pair]?.destroy()
            }

        treeNodesToModels.inverse[node]?.let { node1 ->
            view.activity.lifecycleScope.launch {
                treeNodesToModels.remove(node1)
                tree.removeNode(node1)
                updateNodes(node1.neighbours, tree.translocation)
                deleteNodes(listOf(node1))
                node?.destroy()
                selectNode(null)
            }
        }

    }

    fun selectNode(node: Node?){
        selectedNode = node
        view.link.isEnabled = node != null
        view.delete.isEnabled = node != null
    }

    fun linkNodes(node1: Node, node2: Node){
        view.activity.lifecycleScope.launch {
            val path1: TreeNode? = treeNodesToModels.inverse[node1]
            val path2: TreeNode? = treeNodesToModels.inverse[node2]

            if (path1 != null || path2 != null){
                tree.addLink(path1!!, path2!!)
                drawLine(node1, node2)
                updateNodes(listOf(path1, path2), tree.translocation)
            }
        }
    }

    fun createPath(){
        view.activity.lifecycleScope.launch {

            hitTest(view.surfaceView).getOrNull()?.let { position ->

                val pathTreeNode = TreeNode.Path(tree.size, position.position)
                tree.addNode(pathTreeNode)
                drawNode(pathTreeNode)
                insertNodes(listOf(pathTreeNode), tree.translocation)
                }
            }
        }

    suspend fun drawNode(treeNode: TreeNode){
        val modelNode = ArNode()
        modelNode.loadModel(
            context = view.activity.applicationContext,
            glbFileLocation = if (treeNode is TreeNode.Entry) "models/cylinder_green.glb" else "models/cylinder.glb",
        )
        modelNode.position = treeNode.position
        modelNode.modelScale = Scale(0.1f)
        modelNode.anchor = modelNode.createAnchor()
        modelNode.model?.let {
            it.isShadowCaster = false
            it.isShadowReceiver = false
        }

        treeNodesToModels[treeNode] = modelNode

        view.surfaceView.addChild(modelNode)
    }

    @SuppressLint("CheckResult")
    fun createEntryByDialog(){
        MaterialDialog(view.activity).show {
            title(text = "Place new entry")
            input(hint = "Number") { _, text ->
               createEntry(text.toString())
            }
            positiveButton(text = "Place")
        }
    }

    @SuppressLint("CheckResult")
    fun initializeByDialog(){
        val activityView = view
        MaterialDialog(view.activity).show {
            title(text = "Initialize tree")
            input(hint = "Number") { _, text ->
                activityView.activity.lifecycleScope.launch {
                    hitTest(activityView.surfaceView).getOrNull()?.let { position ->
                        initialize(text.toString(), position.position)
                    }
                }
            }
            positiveButton(text = "Place")
        }
    }

    @SuppressLint("CheckResult")
    fun pathfindByDialog(){
        MaterialDialog(view.activity).show {
            title(text = "Start and end")
            input(hint = "Number") { _, text ->
                try {
                    val dest = text.split(" ")
                    pathfind(
                        from = dest[0],
                        to = dest[1]
                    )
                }
                catch (e: Exception) {
                    showSnackbar("Bad input")
                }
            }
            positiveButton(text = "Place")
        }
    }

    fun pathfind(from: String, to: String){
        if (tree.entryPoints[from] != null && tree.entryPoints[to] != null) {
            view.activity.lifecycleScope.launch {
                val path = findWay(from, to, tree)
                if (path != null) {
                    drawWay(path)
                } else {
                    showSnackbar("No path found")
                }
            }
        }
        else {
            showSnackbar("Wrong entry points. Available: ${tree.entryPoints.keys}")
        }
    }

    fun drawWay(nodes: List<TreeNode>){
        linksToWayModels.values.forEach { it.destroy() }
        linksToWayModels.clear()


        if (nodes.size > 1){
            for (i in 1 until nodes.size) {
                val node1 = treeNodesToModels[nodes[i-1]]
                val node2 = treeNodesToModels[nodes[i]]

                if (node1 != null && node2 != null) {
                    drawWayLine(node1, node2)
                }
            }
        }

    }

    //TODO объединить createEntry и createPAth
    fun createEntry(number: String) {
        view.activity.lifecycleScope.launch {

            hitTest(view.surfaceView).getOrNull()?.let { position ->

                val entryTreeNode = TreeNode.Entry(number, tree.size, position.position)
                tree.addNode(entryTreeNode)
                drawNode(entryTreeNode)
                insertNodes(listOf(entryTreeNode), tree.translocation)
            }
        }
    }

    fun drawTree(){
        view.activity.lifecycleScope.launch {
            for (node in tree.allPoints.values){
                drawNode(node)
            }
            for (treenode1 in tree.links.keys){
                val node1 = treeNodesToModels[treenode1]!!
                val others = tree.links[treenode1]!!
                for (treenode2 in others) {
                    val node2 = treeNodesToModels[treenode2]!!
                    if (modelsToLinkModels[Pair(node1, node2)] == null ){
                        drawLine(node1, node2)
                    }
                }
            }
        }
    }

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
                node.parent = from
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

                modelsToLinkModels[Pair(from, to)] = node
            }
    }

    fun drawWayLine(from: Node, to: Node){

        val fromVector = from.position.toVector3()
        val toVector = to.position.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = Color(android.graphics.Color.parseColor("#7cfc00"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(view.activity.applicationContext, colorOrange)
            .thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    0.015f, lineLength,
                    Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                val node = ArNode()
                node.setModel(model)
                node.parent = from
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

                linksToWayModels[Pair(from, to)] = node
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

    private fun preload(){
        view.activity.lifecycleScope.launch {
            tree = getTree()
        }
    }

    private suspend fun initialize(entryNumber: String, position: Float3){
        val result = tree.initialize(entryNumber, position)
        if (result.isFailure){
            result.exceptionOrNull()?.message?.let{ showSnackbar(it) }
        }
        else {
            drawTree()
            view.initText.isVisible = false
            view.delete.isEnabled = true
            view.link.isEnabled = true
            view.entry.isEnabled = true
            view.pathfind.isEnabled = true
            view.place.isEnabled = true
            view.rend.isEnabled = true

        }
    }

        private fun showSnackbar(message: String): Unit =
            activity.view.snackbarHelper.showError(activity, message)

        private fun hideSnackbar() = activity.view.snackbarHelper.hide(activity)

    /**
     * Temporary arrays to prevent allocations in [createAnchor].
     */
//    private val convertFloats = FloatArray(4)
//    private val convertFloatsOut = FloatArray(4)

    /** Create an anchor using (x, y) coordinates in the [Coordinates2d.IMAGE_PIXELS] coordinate space. */
//    fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Anchor? {
//        // IMAGE_PIXELS -> VIEW
//        convertFloats[0] = xImage
//        convertFloats[1] = yImage
//        frame.transformCoordinates2d(
//            Coordinates2d.IMAGE_PIXELS,
//            convertFloats,
//            Coordinates2d.VIEW,
//            convertFloatsOut
//        )
//
//        // Conduct a hit test using the VIEW coordinates
//        val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
//        val result = hits.getOrNull(0) ?: return null
//        return result.trackable.createAnchor(result.hitPose)
//    }
}



//data class ARLabeledAnchor(val anchor: Anchor, val label: String)

