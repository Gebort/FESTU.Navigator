package com.example.festunavigator.presentation

import android.annotation.SuppressLint
import android.opengl.Matrix
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.festunavigator.R
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.ml.DetectedText
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.use_cases.*
import com.example.festunavigator.presentation.common.helpers.DisplayRotationHelper
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.*
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.*


/**
 * Renders the HelloAR application into using our example Renderer.
 */
class AppRenderer(
    private val activity: MainActivity,
    val analyzeImage: AnalyzeImage,
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
        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
        //delay in seconds for detected object to settle in
        val smoothDelay = 0.5
    }

    lateinit var view: MainActivityView
    private var state: MainState = MainState.ScanningState.Initialize()

    var selectedNode: Node? = null
    var tree = Tree()
    val treeNodesToModels: MutableBiMap<TreeNode, Node> = mutableBiMapOf()
    val modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()
    val linksToWayModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()
    val labelsNodes = mutableListOf<Node>()

    var lastPlacedLabel: ArNode? = null

    var linkPlacementMode = false

    val displayRotationHelper = DisplayRotationHelper(activity)

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    fun bindView(view: MainActivityView) {
        this.view = view

        selectNode(null)

        preload()

        changeState(MainState.ScanningState.Initialize(), false)

        view.surfaceView.onTouchEvent = {pickHitResult, motionEvent ->

            pickHitResult.node?.let { node ->
                if (!linkPlacementMode) {
                    selectNode(node)

                }
                else {
                    linkNodes(selectedNode!!, node)
                    changeLinkPlacementMode(false)
                }
            }
            true
        }

        view.acceptButton.setOnClickListener {
            if (state is MainState.ConfirmingState){
                (state as MainState.ConfirmingState).result = true
            }
        }

        view.rejectButton.setOnClickListener {
            if (state is MainState.ConfirmingState){
                (state as MainState.ConfirmingState).result = false
            }
        }

        view.delete.setOnClickListener {
            removeNode(selectedNode)
        }

        view.link.setOnClickListener {
            changeLinkPlacementMode(!linkPlacementMode)
        }

        view.place.setOnClickListener {
            view.activity.lifecycleScope.launch {
                createNode()
            }
        }

        view.entry.setOnClickListener {
//            createEntryByDialog()
            changeState(MainState.ScanningState.EntryCreation())

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

    }

    private fun initializeByScan(){
            changeState(MainState.ScanningState.Initialize(), false)
    }

   private suspend fun tryGetDetectedObject(): DetectedText? {
           val session = view.surfaceView.arSession ?: return null
           val frame = session.currentFrame?.frame ?: return null
           val camera = frame.camera
           camera.getViewMatrix(viewMatrix, 0)
           camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
           Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
           if (camera.trackingState != TrackingState.TRACKING) {
               return null
           }
           val cameraImage = frame.tryAcquireCameraImage()
           if (cameraImage != null) {
               val cameraId = session.cameraConfig.cameraId
               val imageRotation =
                   displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
               val displaySize = Pair(
                   view.surfaceView.arSession!!.displayWidth,
                   view.surfaceView.arSession!!.displayHeight
               )
               val detectedResult = analyzeImage(
                   cameraImage,
                   imageRotation,
                   DESIRED_CROP,
                   displaySize
               )

               cameraImage.close()

               detectedResult.getOrNull()?.let {
                   return DetectedText(it, frame)
               }
               return null
           }
       cameraImage?.close()
       return null

   }

    private fun hitTestDetectedObject(detectedText: DetectedText): OrientatedPosition? {

        val detectedObject = detectedText.detectedObjectResult
        return useHitTest(
            detectedObject.centerCoordinate.x,
            detectedObject.centerCoordinate.y,
        )
            .getOrNull()
    }

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

        if (state is MainState.ScanningState) {
            (state as MainState.ScanningState).let { state ->
                if (state.currentScanSmoothDelay > 0) {
                    state.currentScanSmoothDelay -= frameTime.intervalSeconds
                }

                if (!state.scanningNow) {
                    if (state.scanningJob?.isActive != true) {
                        state.scanningJob =
                            view.activity.lifecycleScope.launch {
                                if (state.currentScanSmoothDelay <= 0 && state.lastDetectedObject != null) {

                                    val orientatedPos = hitTestDetectedObject(state.lastDetectedObject!!)
                                    if (orientatedPos != null){
                                        val node = placeLabel(
                                            state.lastDetectedObject!!.detectedObjectResult.label,
                                            orientatedPos
                                        )
                                        val confirmationObject = ConfirmationObject(
                                            label = state.lastDetectedObject!!.detectedObjectResult.label,
                                            pos = orientatedPos,
                                            node = node
                                        )
                                        when (state) {
                                            is MainState.ScanningState.Initialize -> {
                                                changeState(MainState.ConfirmingState.InitializeConfirm(confirmationObject))
                                            }
                                            is MainState.ScanningState.EntryCreation -> {
                                                changeState(MainState.ConfirmingState.EntryConfirm(confirmationObject))
                                            }
                                            else -> {
                                                throw Exception("Unknown state")
                                            }
                                        }
                                    }
                                    else {
                                        state.currentScanSmoothDelay = smoothDelay
                                    }

                                } else {
                                    state.scanningNow = true
                                    val detectedObject = tryGetDetectedObject()
                                    if (state.lastDetectedObject == null) {
                                        state.lastDetectedObject = detectedObject
                                        state.currentScanSmoothDelay = smoothDelay
                                    } else if (detectedObject == null) {
                                        state.currentScanSmoothDelay = smoothDelay
                                    } else {
                                        if (state.lastDetectedObject!!.detectedObjectResult.label !=
                                            detectedObject.detectedObjectResult.label
                                        ) {
                                            state.currentScanSmoothDelay = smoothDelay
                                        }
                                        state.lastDetectedObject = detectedObject
                                    }
                                    state.scanningNow = false
                                }
                            }
                    }
                }
            }
        }
        if (state is MainState.ConfirmingState){
            (state as MainState.ConfirmingState).let { state ->
                if (state.result != null) {
                    when (state.result) {
                        false -> {
                            lastPlacedLabel?.destroy()
                            lastPlacedLabel = null
                            when (state.previous){
                                is MainState.ScanningState.Initialize -> {
                                    changeState(MainState.ScanningState.Initialize())
                                }
                                is MainState.ScanningState.EntryCreation -> {
                                    changeState(MainState.ScanningState.EntryCreation())
                                }
                                else -> {
                                    throw Exception("Unknown state")
                                }
                            }
                        }
                        true -> {
                            changeState(MainState.Routing)
                        }
                        else -> {
                            throw Exception ("Unexpected null state result")
                        }
                    }
                }
            }
        }

        }

    private suspend fun placeLabel(label: String, pos: OrientatedPosition): ArNode {
        var node: ArNode? = null
        ViewRenderable.builder()
            .setView(view.activity, R.layout.text_sign)
            .build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.let {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                }
                val cardView = renderable.view as CardView
                val textView: TextView = cardView.findViewById(R.id.signTextView)
                textView.text = label
                val textNode = ArNode().apply {
                    setModel(
                        renderable = renderable
                    )
                    modelPosition = Float3(0f, 0f, 0f)
                    position = Position(pos.position.x, pos.position.y, pos.position.z)
                    quaternion = pos.orientation

                      //  anchor = pos.hitResult.createAnchor()
                }

                lastPlacedLabel?.destroy()
                lastPlacedLabel = textNode

                view.surfaceView.addChild(textNode)
                node = textNode
            }
            .await()

        return node!!
    }

    private suspend fun proceedConfirmationObject(){
        when (state) {
            is MainState.ConfirmingState.InitializeConfirm -> {
                (state as MainState.ConfirmingState).confirmationObject.let {
                    initialize(it.label, it.node.position)
                }
            }
            is MainState.ConfirmingState.EntryConfirm -> {
                (state as MainState.ConfirmingState).confirmationObject.let {
                    createNode(it.label, it.node.position, it.pos.orientation.toRotation().toVector3())
                }
            }
            else -> {
                throw Exception("State is not 'confirm', cant proceed placed label")
            }
        }
    }

    private fun changeLinkPlacementMode(link: Boolean){
        linkPlacementMode = link
        view.link.text = if (link) "Cancel" else "Link"
    }

    private fun removeNode(node: Node?){
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
                val nodesForUpdate = node1.neighbours.toMutableList()
                tree.removeNode(node1)
                updateNodes(nodesForUpdate, tree.translocation)
                deleteNodes(listOf(node1))
                node?.destroy()
                selectNode(null)
            }
        }

    }

    private fun selectNode(node: Node?){
        selectedNode = node
        view.link.isEnabled = node != null
        view.delete.isEnabled = node != null
    }

    private fun setConfirmClickListeners(onAccept: () -> Unit, onReject: () -> Unit){
        view.acceptButton.setOnClickListener {
            onAccept()
        }
        view.rejectButton.setOnClickListener {
            onReject()

        }

    }

    private fun changeState(state: MainState, addToStack: Boolean = true){
        if (addToStack){
            state.previous = this.state
        }
        this.state = state
        when (state) {
            is MainState.ScanningState.Initialize -> {
                //view.routeLayout.isGone = true
                view.confirmLayout.isGone = true
                view.scanImage.isGone = false
                view.scanText.isGone = false
            }
            is MainState.ConfirmingState.InitializeConfirm -> {
                //view.routeLayout.isGone = true
                view.confirmLayout.isGone = false
                view.scanImage.isGone = true
                view.scanText.isGone = true
            }
            is MainState.ScanningState.EntryCreation -> {
                //view.routeLayout.isGone = true
                view.confirmLayout.isGone = true
                view.scanImage.isGone = false
                view.scanText.isGone = false

            }
            is MainState.ConfirmingState.EntryConfirm -> {
                //view.routeLayout.isGone = true
                view.confirmLayout.isGone = false
                view.scanImage.isGone = true
                view.scanText.isGone = true
            }
            is MainState.Routing -> {
                //view.routeLayout.isGone = false
                view.confirmLayout.isGone = true
                view.scanImage.isGone = true
                view.scanText.isGone = true

            }
            else -> {
                throw Exception("Unknown state")
            }
        }
    }

    private fun rollbackState(){
        if (state.previous != null){
            changeState(state.previous!!, false)
        }
        else {
            throw Exception("Null previous state")
        }
    }

    private fun linkNodes(node1: Node, node2: Node){
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

    private suspend fun drawNode(treeNode: TreeNode, anchor: Anchor? = null){
        val modelNode = ArNode()
        modelNode.loadModel(
            context = view.activity.applicationContext,
            glbFileLocation = if (treeNode is TreeNode.Entry) "models/cylinder_green.glb" else "models/cylinder.glb",
        )
        modelNode.position = treeNode.position
        modelNode.modelScale = Scale(0.1f)
        modelNode.anchor = modelNode.createAnchor()
//        anchor?.let {
//            modelNode.anchor = it
//        }
        modelNode.model?.let {
            it.isShadowCaster = false
            it.isShadowReceiver = false
        }

        treeNodesToModels[treeNode] = modelNode

        view.surfaceView.addChild(modelNode)
    }

    @SuppressLint("CheckResult")
    fun createEntryByDialog(){
        val activity = view.activity
        MaterialDialog(view.activity).show {
            title(text = "Place new entry")
            input(hint = "Number") { _, text ->
                activity.lifecycleScope.launch {
                    createNode(text.toString())
                }
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
                        useHitTest().getOrNull()?.let { position ->
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

    private fun drawWay(nodes: List<TreeNode>){
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

    private suspend fun createNode(
        number: String? = null,
        position: Float3? = null,
        forwardVector3: Vector3? = null
    ) {
            val treeNode = if (position == null) {
                val result = useHitTest().getOrNull()
                if (result != null) {
                    tree.addNode(result.position, number, forwardVector3)
                } else {
                    null
                }
            } else {
                tree.addNode(position, number, forwardVector3)
            }

            treeNode?.let {
                insertNodes(listOf(treeNode), tree.translocation)
                drawNode(treeNode)

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
    private fun Frame.tryAcquireCameraImage() = try {
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
            val rotationVector = result.getOrNull()
            rotationVector?.let {
                view.surfaceView.camera.rotation = it.toFloat3()
            }
            view.surfaceView.camera.worldRotation
            drawTree()

            view.delete.isEnabled = true
            view.link.isEnabled = true
            view.entry.isEnabled = true
            view.pathfind.isEnabled = true
            view.place.isEnabled = true

        }

    }

    private fun useHitTest(
        x: Float = view.surfaceView.arSession!!.displayWidth / 2f,
        y: Float = view.surfaceView.arSession!!.displayHeight / 2f,
        currentFrame: Frame? = null
    ): Result<OrientatedPosition> {
        val frame = currentFrame ?:
            view.surfaceView.currentFrame?.frame ?: return Result.failure(Exception("Frame null"))
        val cameraPos = view.surfaceView.camera.worldPosition
        val result = hitTest(frame, Float2(x, y), cameraPos, view.surfaceView)
//        result.getOrNull()?.let {
//            val node1 = ArNode()
//            node1.position = cameraPos
//            val node2 = ArNode()
//            node2.position = it.position
//            drawLine(node1, node2)
//        }
        return result
    }

        private fun showSnackbar(message: String): Unit =
            activity.view.snackbarHelper.showError(activity, message)

}




//data class ARLabeledAnchor(val anchor: Anchor, val label: String)

