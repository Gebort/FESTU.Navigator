package com.example.festunavigator.presentation

import android.annotation.SuppressLint
import android.opengl.Matrix
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.festunavigator.R
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.ml.DetectedText
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.use_cases.*
import com.example.festunavigator.presentation.common.adapters.EntriesAdapter
import com.example.festunavigator.presentation.common.adapters.EntryItem
import com.example.festunavigator.presentation.common.helpers.AnimationHelper
import com.example.festunavigator.presentation.common.helpers.DisplayRotationHelper
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.example.festunavigator.presentation.common.helpers.NumberLocationHelper
import com.google.android.material.snackbar.Snackbar
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
        const val smoothDelay = 0.5
    }

    lateinit var view: MainActivityView
    private var state: MainState = MainState.Starting

    var selectedNode: Node? = null
    var tree = Tree()

    var lastPlacedLabel: ArNode? = null

    var linkPlacementMode = false

    private var adapter: EntriesAdapter = EntriesAdapter(){ number ->
        processSearchResult(number)
    }

    val drawerHelper = DrawerHelper(activity)
    val displayRotationHelper = DisplayRotationHelper(activity)
    val animationHelper = AnimationHelper()
    val numberLocationHelper = NumberLocationHelper(activity.applicationContext)

    val linksToWayModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()
    val treeNodesToModels: MutableBiMap<TreeNode, Node> = mutableBiMapOf()
    val modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()
    val labelsNodes = mutableListOf<Node>()

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

        changeState(MainState.Starting)

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
            changeState(MainState.ScanningState.EntryCreation())

        }

        view.surfaceView.onFrame = { frameTime ->
            onDrawFrame(frameTime)

        }

        view.init.setOnClickListener {
            initializeByScan()
        }

        view.pathfind.setOnClickListener {
            if (tree.initialized) {
                pathfindByDialog()
            }
            else {
                showSnackbar("Tree isnt init")
            }
        }

        view.fromInput.setOnFocusChangeListener { _, b ->
            if (b) {
                (state as? MainState.Routing.Going)?.let {
                    view.fromInput.isActivated = false
                    view.fromInput.clearFocus()
                    changeState(MainState.Routing.Choosing(
                        true,
                        startNumber = it.startNumber,
                        endNumber = it.endNumber
                    ))
                }
                }

        }

        view.toInput.setOnFocusChangeListener { _, b ->
            if (b) {
                (state as? MainState.Routing.Going)?.let {
                    view.toInput.isActivated = false
                    view.toInput.clearFocus()
                    changeState(MainState.Routing.Choosing(
                        false,
                        startNumber = it.startNumber,
                        endNumber = it.endNumber
                    ))
                }
            }
        }

        view.searchInput.doOnTextChanged { text, _, _, _ ->
            adapter.applyFilter(text.toString())
        }

        view.searchInput.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                processSearchResult(view.searchInput.text.toString())
                handled = true
            }
            handled
        })

    }

    fun backPressed(){
        if (state.previous != null){
            rollbackState()
        }
        else {
            view.activity.finish()
        }
    }

    private fun initializeByScan(){
            changeState(MainState.ScanningState.Initialize())
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
                            rollbackState()
                        }
                        true -> {
                            when (state) {
                                is MainState.ConfirmingState.InitializeConfirm -> {
                                    if (state.confirmationJob == null ||
                                        state.confirmationJob!!.isCompleted)
                                        {
                                        state.confirmationJob =
                                            view.activity.lifecycleScope.launch {
                                                val isInit = initialize(
                                                    state.confirmationObject.label,
                                                    state.confirmationObject.pos.position
                                                )
                                                if (isInit) {
                                                    changeState(MainState.Routing.Going(
                                                        startNumber = state.confirmationObject.label
                                                    ))
                                                } else {
                                                    lastPlacedLabel?.destroy()
                                                    lastPlacedLabel = null
                                                    rollbackState()
                                                }
                                            }
                                    }
                                }
                                is MainState.ConfirmingState.EntryConfirm -> {
                                    view.activity.lifecycleScope.launch {
                                        val entry = state.confirmationObject
                                        createNode(
                                            entry.label,
                                            entry.pos.position,
                                            entry.pos.orientation.toRotation().toVector3()
                                        )
                                        changeState(MainState.Routing.Going())
                                    }
                                }
                                else -> {
                                    throw Exception("Unknown confirming state")
                                }
                            }
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

    private fun changeState(state: MainState, clearStack: Boolean = false){
        if (!clearStack){
            state.previous = this.state
        }
        this.state = state
        when (state) {
            is MainState.Starting -> {
                selectNode(null)
                preload()
                view.confirmLayout.doOnLayout { animationHelper.placeViewOut(view.confirmLayout) }
                view.routeLayout.doOnLayout { animationHelper.placeViewOut(view.routeLayout) }
                view.routeBigLayout.doOnLayout { animationHelper.placeViewOut(view.routeBigLayout) }
                view.entryRecyclerView.adapter = adapter
                view.entryRecyclerView.layoutManager = LinearLayoutManager(
                    view.activity.applicationContext
                )
                changeState(MainState.ScanningState.Initialize(), true)
            }
            is MainState.ScanningState.Initialize -> {
                animationHelper.let {
                    it.viewHideInput(view.searchInput, view.activity.applicationContext)
                    it.slideViewDown(view.routeBigLayout, false)
                    it.slideViewDown(view.routeLayout)
                    it.slideViewDown(view.confirmLayout)
                    it.fadeShow(view.scanImage)
                    it.fadeShow(view.scanText)
                }

            }
            is MainState.ConfirmingState.InitializeConfirm -> {
                animationHelper.let {
                    it.viewHideInput(view.searchInput, view.activity.applicationContext)
                    it.slideViewDown(view.routeBigLayout, false)
                    it.slideViewDown(view.routeLayout)
                    it.slideViewUp(view.confirmLayout)
                    it.fadeHide(view.scanImage)
                    it.fadeHide(view.scanText)
                }

            }
            is MainState.ScanningState.EntryCreation -> {
                animationHelper.let {
                    it.viewHideInput(view.searchInput, view.activity.applicationContext)
                    it.slideViewDown(view.routeBigLayout, false)
                    it.slideViewDown(view.routeLayout)
                    it.slideViewDown(view.confirmLayout)
                    it.fadeShow(view.scanImage)
                    it.fadeShow(view.scanText)
                }
            }
            is MainState.ConfirmingState.EntryConfirm -> {
                animationHelper.let {
                    it.viewHideInput(view.searchInput, view.activity.applicationContext)
                    it.slideViewDown(view.routeBigLayout, false)
                    it.slideViewDown(view.routeLayout)
                    it.slideViewUp(view.confirmLayout)
                    it.fadeHide(view.scanImage)
                    it.fadeHide(view.scanText)
                }
            }
            is MainState.Routing.Going -> {
                animationHelper.let {
                    it.viewHideInput(view.searchInput, view.activity.applicationContext)
                    it.slideViewDown(view.routeBigLayout, false)
                    it.slideViewUp(view.routeLayout)
                    it.slideViewDown(view.confirmLayout)
                    it.fadeHide(view.scanImage)
                    it.fadeHide(view.scanText)
                }
                view.fromInput.setText(state.startNumber)
                view.toInput.setText(state.endNumber)

                if (state.startNumber != null && state.endNumber != null){
                    if (state.startNumber == state.endNumber){
                        state.endNumber = null
                    }
                    else {
                        pathfind(state.startNumber!!, state.endNumber!!)
                    }
                }

            }
            is MainState.Routing.Choosing -> {
                view.searchInput.setText("")
                view.searchLayout.error = null
                view.searchLayout.hint =
                    if (state.choosingStart) view.activity.getString(R.string.from)
                    else view.activity.getString(R.string.to)
                animationHelper.let {
                    it.slideViewDown(view.routeLayout)
                    it.slideViewDown(view.confirmLayout)
                    it.fadeHide(view.scanImage)
                    it.fadeHide(view.scanText)
                    it.slideViewUp(view.routeBigLayout, false){
                        animationHelper.viewRequestInput(
                            view.searchInput,
                            view.activity.applicationContext
                        )
                        view.activity.lifecycleScope.launch {
                            var entriesList = listOf<EntryItem>()
                            withContext(Dispatchers.IO){
                                entriesList = tree.entryPoints.keys.map { number ->
                                    EntryItem(number, numberLocationHelper(number))
                                }
                            }
                            adapter.changeList(entriesList)
                        }
                    }
                }
            }
            else -> {
                throw Exception("Unknown state")
            }
        }
    }

    private fun rollbackState(){
        if (state.previous != null){
            val newState = when (state.previous) {
                is MainState.Routing.Choosing -> {
                    state.previous!!
                }
                is MainState.Routing.Going -> {
                    if (state is MainState.Routing.Choosing){
                        (state as MainState.Routing.Choosing).let {
                            if (it.done){
                                MainState.Routing.Going(
                                    startNumber = it.startNumber,
                                    endNumber = it.endNumber
                                )
                            }
                            else {
                                state.previous!!
                            }
                        }
                    }
                    else {
                        state.previous!!
                    }
                }
                is MainState.ScanningState.EntryCreation-> {
                    MainState.ScanningState.EntryCreation()
                }
                is MainState.ScanningState.Initialize-> {
                    MainState.ScanningState.Initialize()
                }
                is MainState.ConfirmingState.InitializeConfirm-> {
                    MainState.ConfirmingState.InitializeConfirm(
                        (state.previous as MainState.ConfirmingState.InitializeConfirm).confirmationObject
                    )
                }
                is MainState.ConfirmingState.EntryConfirm-> {
                    MainState.ConfirmingState.EntryConfirm(
                        (state.previous as MainState.ConfirmingState.EntryConfirm).confirmationObject
                    )
                }
                else -> {
                    throw Exception("Rolling back to starting state")
                }
            }
            newState.previous = if (state.previous == null) null else state.previous!!.previous
            changeState(newState, true)
        }
        else {
            throw Exception("Null previous state")
        }
    }

    private fun linkNodes(node1: Node, node2: Node){
        view.activity.lifecycleScope.launch {
            val path1: TreeNode? = treeNodesToModels.inverse[node1]
            val path2: TreeNode? = treeNodesToModels.inverse[node2]

            if (path1 != null && path2 != null){
                tree.addLink(path1, path2)
                drawerHelper.drawLine(
                    node1,
                    node2,
                    modelsToLinkModels,
                    view.surfaceView
                )
                updateNodes(listOf(path1, path2), tree.translocation)
            }
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
                    drawerHelper.drawWay(
                        path,
                        linksToWayModels,
                        treeNodesToModels,
                        view.surfaceView
                    )
                } else {
                    showSnackbar("No path found")
                }
            }
        }
        else {
            showSnackbar("Wrong entry points. Available: ${tree.entryPoints.keys}")
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
                drawerHelper.drawNode(
                    treeNode,
                    treeNodesToModels,
                    view.surfaceView
                )

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

    private fun processSearchResult(number: String) {
        (state as? MainState.Routing.Choosing)?.let {
            if (tree.entryPoints.containsKey(number)){
                view.searchLayout.error = null
            }
            else {
                view.searchLayout.error = view.activity.getString(R.string.incorrect_number)
                animationHelper.viewHideInput(
                    view.searchInput,
                    view.activity.applicationContext
                )
                animationHelper.viewRequestInput(
                    view.searchInput,
                    view.activity.applicationContext
                )
                return
            }
            if (it.choosingStart) {
                it.startNumber = number
                if (it.startNumber == it.endNumber)
                    it.endNumber = null
            }
            else
                it.endNumber = number
            if (it.startNumber == it.endNumber)
                it.startNumber = null
            it.done = true
            rollbackState()
        }
    }

    private fun preload(){
        view.activity.lifecycleScope.launch {
            tree = getTree()
        }
    }

    private suspend fun initialize(entryNumber: String, position: Float3): Boolean{
        var result: Result<Vector3>
        withContext(Dispatchers.IO) {
            result = tree.initialize(entryNumber, position)
        }
        if (result.isFailure){
            result.exceptionOrNull()?.message?.let{ showSnackbar(it) }
            return false
        }
        else {
            val rotationVector = result.getOrNull()
            rotationVector?.let {
                view.surfaceView.camera.rotation = it.toFloat3()
            }
            view.surfaceView.camera.worldRotation
            drawerHelper.drawTree(
                tree,
                treeNodesToModels,
                modelsToLinkModels,
                view.surfaceView
            )

            view.delete.isEnabled = true
            view.link.isEnabled = true
            view.entry.isEnabled = true
            view.pathfind.isEnabled = true
            view.place.isEnabled = true
            return true
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
        return result
    }

        private fun showSnackbar(message: String) {
            val anchorView = when (state) {
                is MainState.ConfirmingState -> view.confirmLayout
                is MainState.Routing.Going -> view.routeLayout
                else -> null
            }
            Snackbar.make(view.surfaceView, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(anchorView)
                .show()
        }

}
