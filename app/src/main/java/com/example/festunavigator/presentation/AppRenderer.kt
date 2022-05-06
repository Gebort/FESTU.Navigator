package com.example.festunavigator.presentation

import android.opengl.Matrix
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.festunavigator.R
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.ml.DetectedText
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.use_cases.*
import com.example.festunavigator.presentation.common.adapters.EntriesAdapter
import com.example.festunavigator.presentation.common.adapters.EntryItem
import com.example.festunavigator.presentation.common.helpers.*
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.*


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
    val hitTest: HitTest,
    val destinationDesc: GetDestinationDesc = GetDestinationDesc()
    ) : DefaultLifecycleObserver, CoroutineScope by MainScope() {

    companion object {
        const val TAG = "HelloArRenderer"
        const val ADMIN_MODE = "ADMIN"
        const val USER_MODE = "USER"
        val mode = ADMIN_MODE
        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
        //delay in seconds for detected object to settle in
        const val smoothDelay = 0.5
    }

    lateinit var view: MainActivityView
    var state: MainState = MainState.Starting
        private set

    var selectedNode: Node? = null
    var tree = Tree()

    var linkPlacementMode = false

    var adapter: EntriesAdapter = EntriesAdapter(){ number ->
        processSearchResult(number)
    }

    val viewStateHelper = ViewStateHelper(this)
    val drawerHelper = DrawerHelper(activity)
    val displayRotationHelper = DisplayRotationHelper(activity)
    val animationHelper = AnimationHelper()

    val treeNodesToModels: MutableBiMap<TreeNode, Node> = mutableBiMapOf()
    val modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()

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

        if (mode == ADMIN_MODE) {
            view.adminPanel.isVisible = true
        }
        else {
            view.adminPanel.isGone = true
        }

        view.surfaceView.onTouchEvent = {pickHitResult, motionEvent ->

            if (mode == ADMIN_MODE) {
                pickHitResult.node?.let { node ->
                    if (!linkPlacementMode) {
                        selectNode(node)

                    } else {
                        linkNodes(selectedNode!!, node)
                        changeLinkPlacementMode(false)
                    }
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

        view.fromInput.setOnFocusChangeListener { _, b ->
            if (b) {
                (state as? MainState.Routing.Going)?.let {
                    view.fromInput.isActivated = false
                    view.fromInput.clearFocus()
                    changeState(MainState.Routing.Choosing(
                        true,
                        startLabel = it.startLabel,
                        endLabel = it.endLabel
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
                        startLabel = it.startLabel,
                        endLabel = it.endLabel
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

    private fun hitTestDetectedObject(detectedText: DetectedText): HitTestResult? {

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

                                    val res = hitTestDetectedObject(state.lastDetectedObject!!)
                                    if (res != null){
                                        val confirmationObject = LabelObject(
                                            label = state.lastDetectedObject!!.detectedObjectResult.label,
                                            pos = res.orientatedPosition,
                                            anchor = res.hitResult.createAnchor()
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
                            rollbackState()
                        }
                        true -> {
                            when (state) {
                                is MainState.ConfirmingState.InitializeConfirm -> {
                                    if (state.confirmationJob == null ||
                                        !state.confirmationJob!!.isActive)
                                        {
                                        state.confirmationJob =
                                            view.activity.lifecycleScope.launch {
                                                val isInit = initialize(
                                                    state.labelObject.label,
                                                    state.labelObject.pos.position,
                                                    state.labelObject.pos.orientation
                                                )
                                                if (isInit) {
                                                    state.labelObject.node?.destroy()
                                                    changeState(MainState.Routing.Going(
                                                        startLabel = state.labelObject
                                                    ))
                                                } else {
                                                    rollbackState()
                                                }
                                            }
                                    }
                                }
                                is MainState.ConfirmingState.EntryConfirm -> {
                                    if (state.confirmationJob == null ||
                                        !state.confirmationJob!!.isActive)
                                        {
                                        state.confirmationJob =
                                            view.activity.lifecycleScope.launch {
                                                val entry = state.labelObject
                                                createNode(
                                                    entry.label,
                                                    entry.pos.position,
                                                    entry.pos.orientation
                                                )
                                                changeState(MainState.Routing.Going())
                                            }
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
                modelsToLinkModels.remove(pair)
            }

        val b = treeNodesToModels.inverse[node]
        showSnackbar((b?.id ?: -1).toString())

        treeNodesToModels.inverse[node]?.let { node1 ->
            view.activity.lifecycleScope.launch {
                treeNodesToModels.remove(node1)
                val nodesForUpdate = tree.getNodes(node1.neighbours.toMutableList())
                tree.removeNode(node1)
                updateNodes(nodesForUpdate, tree.translocation, tree.rotation, tree.pivotPosition)
                deleteNodes(listOf(node1))
                node?.destroy()
                selectNode(null)
            }
        }

    }

    fun selectNode(node: Node?){
        selectedNode = node
        val treeNode = treeNodesToModels.inverse[node]

        view.link.isEnabled = node != null
        view.delete.isEnabled = node != null
    }

    private fun changeState(state: MainState, clearStack: Boolean = false){
        this.state.let {
            when (it) {
                is MainState.Starting -> {}

                is MainState.ScanningState.Initialize -> {
                    it.scanningJob?.cancel()
                    viewStateHelper.onInitializeEnd()

                }
                is MainState.ConfirmingState.InitializeConfirm -> {
                    it.labelObject.node?.destroy()
                    it.confirmationJob?.cancel()
                    it.confirmationObjectJob?.cancel()
                    viewStateHelper.onInitializeConfirmEnd()
                }
                is MainState.ScanningState.EntryCreation -> {
                    viewStateHelper.onEntryCreationEnd()
                    it.scanningJob?.cancel()
                }
                is MainState.ConfirmingState.EntryConfirm -> {
                    it.labelObject.node?.destroy()
                    it.labelObject.node = null
                    it.confirmationJob?.cancel()
                    it.confirmationObjectJob?.cancel()
                    viewStateHelper.onEntryConfirmEnd()
                }
                is MainState.Routing.Going -> {
                    it.wayBuildingJob?.cancel()
                    it.startPlacingJob?.cancel()
                    it.endPlacingJob?.cancel()
                    it.wayNodes.forEach { node -> node.destroy() }
                    it.endLabel?.node?.destroy()
                    it.startLabel?.node?.destroy()
                    viewStateHelper.onGoingEnd()
                }
                is MainState.Routing.Choosing -> {
                    viewStateHelper.onChoosingEnd()
                }
                else -> {
                    throw Exception("Unknown state")
                }
            }
        }

        if (!clearStack){
            state.previous = this.state
        }
        this.state = state

        when (state) {
            is MainState.Starting -> {
                selectNode(null)
                preload()
                viewStateHelper.onStartingStart()
                changeState(MainState.ScanningState.Initialize(), true)
            }
            is MainState.ScanningState.Initialize -> {
                viewStateHelper.onInitializeStart()

            }
            is MainState.ConfirmingState.InitializeConfirm -> {
                viewStateHelper.onInitializeConfirmStart()
                state.confirmationObjectJob?.cancel()
                state.confirmationObjectJob = view.activity.lifecycleScope.launch {
                    state.labelObject.node = drawerHelper.placeLabel(
                        state.labelObject.label,
                        state.labelObject.pos,
                        view.surfaceView,
//                        state.confirmationObject.pos.hitResult.createAnchor()
                    )

                }
            }
            is MainState.ScanningState.EntryCreation -> {
                viewStateHelper.onEntryCreationStart()
            }
            is MainState.ConfirmingState.EntryConfirm -> {
                viewStateHelper.onEntryConfirmStart()
                state.confirmationObjectJob?.cancel()
                state.confirmationObjectJob = view.activity.lifecycleScope.launch {
                    state.labelObject.node = drawerHelper.placeLabel(
                        state.labelObject.label,
                        state.labelObject.pos,
                        view.surfaceView,
//                        state.confirmationObject.pos.hitResult.createAnchor()
                    )

                }
            }
            is MainState.Routing.Going -> {
                viewStateHelper.onGoingStart()
                view.fromInput.setText(state.startLabel?.label)
                view.toInput.setText(state.endLabel?.label)

                if (state.startLabel != null && state.endLabel != null){
                    if (state.startLabel == state.endLabel){
                        state.endLabel = null
                    }
                    else {
                        view.destinationText.isVisible = true
                        view.destinationText.text =
                            "${view.activity.getString(R.string.going)} ${destinationDesc(state.endLabel!!.label, view.activity.applicationContext)}"
                        state.wayBuildingJob?.cancel()
                        state.wayBuildingJob = view.activity.lifecycleScope.launch {
                            state.wayNodes.clear()
                            pathfind(state.startLabel!!.label, state.endLabel!!.label, state.wayNodes)
                        }
                    }
                }
                if (state.endLabel == null){
                    view.destinationText.isGone = true
                }

                if (state.startLabel != null){
                    state.startLabel?.let {
                        state.startPlacingJob?.cancel()
                        state.startPlacingJob = view.activity.lifecycleScope.launch {
                            it.node = drawerHelper.placeLabel(
                                it.label,
                                it.pos,
                                view.surfaceView,
                            )
                        }
                    }
                }
                if (state.endLabel != null){
                    state.endLabel?.let {
                        state.endPlacingJob?.cancel()
                        state.endPlacingJob = view.activity.lifecycleScope.launch {
                            it.node = drawerHelper.placeLabel(
                                it.label,
                                it.pos,
                                view.surfaceView,
                            )
                        }
                    }
                }

            }
            is MainState.Routing.Choosing -> {
                view.searchInput.setText("")
                view.searchLayout.error = null
                view.searchLayout.hint =
                    if (state.choosingStart) view.activity.getString(R.string.from)
                    else view.activity.getString(R.string.to)
                viewStateHelper.onChoosingStart()
                view.activity.lifecycleScope.launch {
                    var entriesList = listOf<EntryItem>()
                    withContext(Dispatchers.IO){
                        entriesList = tree.getEntriesNumbers().map { number ->
                            EntryItem(number, destinationDesc(number, view.activity.applicationContext))
                        }
                    }
                    adapter.changeList(entriesList)
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
                                    startLabel = it.startLabel,
                                    endLabel = it.endLabel
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
                    val confObject = (state.previous as MainState.ConfirmingState.InitializeConfirm)
                        .labelObject
                    MainState.ConfirmingState.InitializeConfirm(
                        confObject
                    )
                }
                is MainState.ConfirmingState.EntryConfirm-> {
                    MainState.ConfirmingState.EntryConfirm(
                        (state.previous as MainState.ConfirmingState.EntryConfirm).labelObject
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
                val ok = tree.addLink(path1, path2)
                if (ok) {
                    drawerHelper.drawLine(
                        node1,
                        node2,
                        modelsToLinkModels,
                        view.surfaceView
                    )
                    updateNodes(listOf(path1, path2), tree.translocation, tree.rotation, tree.pivotPosition)
                }
            }
        }
    }

    private suspend fun pathfind(from: String, to: String, wayNodes: MutableList<ArNode>){
        if (tree.getEntry(from) != null && tree.getEntry(to) != null) {
                val path = findWay(from, to, tree)
                if (path != null) {
                    drawerHelper.drawWay(
                        path,
                        wayNodes,
                        view.surfaceView
                    )
                } else {
                    showSnackbar("No path found")
                }
        }
        else {
            showSnackbar("Wrong entry points. Available: ${tree.getEntriesNumbers()}")
        }
    }

    private suspend fun createNode(
        number: String? = null,
        position: Float3? = null,
        orientation: Quaternion? = null
    ) {
            if (position == null) {
                val result = useHitTest().getOrNull()
                if (result != null) {
                    val treeNode = tree.addNode(
                        result.orientatedPosition.position,
                        number,
                        orientation
                    )
                    treeNode.let {
                        insertNodes(listOf(treeNode), tree.translocation, tree.rotation, tree.pivotPosition)
                        drawerHelper.drawNode(
                            treeNode,
                            treeNodesToModels,
                            view.surfaceView,
                            result.hitResult.createAnchor()
                        )
                    }
                } else {
                    return
                }
            } else {
                val treeNode = tree.addNode(
                    position,
                    number,
                    orientation
                )
                treeNode.let {
                    insertNodes(listOf(treeNode), tree.translocation, tree.rotation, tree.pivotPosition)
                    drawerHelper.drawNode(
                        treeNode,
                        treeNodesToModels,
                        view.surfaceView
                    )
                }
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
            if (tree.hasEntry(number)){
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

            val treeNode = tree.getEntry(number)!!
            val labelObject = LabelObject(
                label = treeNode.number,
                pos = OrientatedPosition(treeNode.position, treeNode.forwardVector),
            )

            if (it.choosingStart) {
                it.startLabel = labelObject
                if (it.startLabel == it.endLabel)
                    it.endLabel = null
            }
            else
                it.endLabel = labelObject
            if (it.startLabel == it.endLabel)
                it.startLabel = null
            it.done = true
            rollbackState()
        }
    }

    fun preload(){
        view.activity.lifecycleScope.launch {
            tree = getTree()
        }
    }

    private suspend fun initialize(entryNumber: String, position: Float3, newOrientation: Quaternion): Boolean{
        var result: Result<Unit?>
        withContext(Dispatchers.IO) {
            result = tree.initialize(entryNumber, position, newOrientation)
        }
        if (result.isFailure){
            result.exceptionOrNull()?.message?.let{ showSnackbar(it) }
            return false
        }
        if (mode == ADMIN_MODE) {
            drawerHelper.drawTree(
                tree,
                treeNodesToModels,
                modelsToLinkModels,
                view.surfaceView
            )

            view.delete.isEnabled = true
            view.link.isEnabled = true
            view.entry.isEnabled = true
            view.place.isEnabled = true
        }
            return true
        }


    private fun useHitTest(
        x: Float = view.surfaceView.arSession!!.displayWidth / 2f,
        y: Float = view.surfaceView.arSession!!.displayHeight / 2f,
        currentFrame: Frame? = null
    ): Result<HitTestResult> {
        val frame = currentFrame ?:
            view.surfaceView.currentFrame?.frame ?: return Result.failure(Exception("Frame null"))
        val cameraPos = view.surfaceView.camera.worldPosition
        val result = hitTest(frame, Float2(x, y), cameraPos, view.surfaceView)
        return result
    }

        private fun showSnackbar(message: String) {
            Snackbar.make(view.surfaceView, message, Snackbar.LENGTH_SHORT)
                .show()
        }

}
