package com.example.festunavigator.presentation.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.databinding.FragmentPreviewBinding
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.pathfinding.Path
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.example.festunavigator.presentation.preview.path_adapter.PathAdapter
import com.example.festunavigator.presentation.preview.state.PathState
import com.example.festunavigator.presentation.scanner.ScannerFragment
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.*
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.destroy
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PreviewFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()

    private val hitTest = App.instance!!.hitTest

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    var selectedNode: Node? = null

    var linkPlacementMode = false

    private val drawerHelper = DrawerHelper(this)

    var endPlacingJob: Job? = null
    var startPlacingJob: Job? = null
    var wayBuildingJob: Job? = null
    val wayNodes: MutableList<ArNode> = mutableListOf()
    var currentPathState = PathState()
    var lastPositionTime = 0L
    lateinit var pathAdapter: PathAdapter


    private var lastConfObject: LabelObject? = null
    private var confObjectJob: Job? = null
    private val treeNodesToModels: MutableBiMap<TreeNode, Node> = mutableBiMapOf()
    private val modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node> = mutableBiMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.sceneView.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.onPause(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //TODO отработать сворачивание приложения
        pathAdapter = PathAdapter(
            drawerHelper,
            binding.sceneView,
            VIEWABLE_PATH_NODES,
            viewLifecycleOwner.lifecycleScope
        )

        binding.sceneView.apply {
            planeRenderer.isVisible = true
            instructions.enabled = false
            onArFrame = { frame ->
                onDrawFrame(frame)
            }

            onTouchEvent = { pickHitResult, _ ->
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
            onArSessionFailed = { exception ->
                val message = when (exception) {
                    is UnavailableArcoreNotInstalledException,
                    is UnavailableUserDeclinedInstallationException -> getString(R.string.install_arcode)
                    is UnavailableApkTooOldException -> getString(R.string.update_arcode)
                    is UnavailableSdkTooOldException -> getString(R.string.update_app)
                    is UnavailableDeviceNotCompatibleException -> getString(R.string.no_arcore_support)
                    is CameraNotAvailableException -> getString(R.string.camera_not_available)
                    is SecurityException -> getString(R.string.provide_camera_permission)
                    else -> getString(R.string.failed_to_create_session)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }

        if (mode == ADMIN_MODE) {
            binding.adminPanel.isVisible = true
        }
        else {
            binding.adminPanel.isGone = true
        }

        selectNode(null)

        binding.deleteButton.setOnClickListener {
            removeNode(selectedNode)
        }

        binding.linkButton.setOnClickListener {
            changeLinkPlacementMode(!linkPlacementMode)
        }

        //TODO нужно вынести административные кнопки в router фрагмент
        binding.placeButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                createNode()
            }
        }

        binding.entryButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt(
                ScannerFragment.SCAN_TYPE,
                ScannerFragment.TYPE_ENTRY
            )
            findNavController().navigate(R.id.action_global_scannerFragment, args = bundle)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.pathState.collectLatest { pathState ->
                    if (currentPathState.path != pathState.path) {
                        //reDrawPath(pathState.path, wayNodes)
                    }
                    if (currentPathState.endLabel != pathState.endLabel){
                        endPlacingJob?.cancel()
                        currentPathState.endLabel?.node?.let {
                            drawerHelper.removeLabelWithAnim(it)
                        }
                        endPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            currentPathState.endLabel?.let {
                                it.node = drawerHelper.placeLabel(
                                    it.label,
                                    it.pos,
                                    binding.sceneView,
                                )
                            }
                        }
                    }
                    if (currentPathState.startLabel != pathState.startLabel){
                        startPlacingJob?.cancel()
                        currentPathState.startLabel?.node?.let {
                            drawerHelper.removeLabelWithAnim(it)
                        }
                        startPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            currentPathState.startLabel?.let {
                                it.node = drawerHelper.placeLabel(
                                    it.label,
                                    it.pos,
                                    binding.sceneView,
                                )
                            }
                        }
                    }
                    currentPathState = pathState
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.mainUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is MainUiEvent.InitSuccess -> {
                            onInitializeSuccess()
                        }
                        is MainUiEvent.InitFailed -> {
                            showSnackbar(getString(R.string.init_failed))
                        }
                        is MainUiEvent.NodeCreated -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                treeNodesToModels[uiEvent.treeNode] = drawerHelper.drawNode(
                                    uiEvent.treeNode,
                                    binding.sceneView,
                                    uiEvent.anchor
                                )
                            }
                        }
                        is MainUiEvent.LinkCreated -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val node1 = treeNodesToModels[uiEvent.node1]
                                val node2 = treeNodesToModels[uiEvent.node2]
                                if (node1 == null || node2 == null){
                                    throw NullPointerException("No nodes found in treeNodesToModels")
                                }
                                drawerHelper.drawLine(
                                    node1,
                                    node2,
                                    modelsToLinkModels,
                                    binding.sceneView
                                )
                            }
                        }
                        is MainUiEvent.NodeDeleted -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                drawerHelper.removeNode(
                                    uiEvent.node,
                                    modelsToLinkModels,
                                    treeNodesToModels
                                )
                            }
                        }
                        is MainUiEvent.PathNotFound -> {
                            showSnackbar(getString(R.string.no_path))
                        }
                }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.confirmationObject.collectLatest { confObject ->
                    confObjectJob?.cancel()
                    confObjectJob = viewLifecycleOwner.lifecycleScope.launch {
                        lastConfObject?.node?.let {
                            drawerHelper.removeArrow(it)
//                            drawerHelper.removeLabelWithAnim(it)
//                            drawerHelper.joinAnimation(it)
                        }
                        confObject?.let {
                            it.node = drawerHelper.placeLabel(
                                confObject.label,
                                confObject.pos,
                                binding.sceneView
                            )
                            lastConfObject = it
                        }
                    }
                }
                }
            }
        }


    private fun onDrawFrame(frame: ArFrame) {

        val camera = frame.camera

        // Handle tracking failures.
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

        mainModel.onEvent(
            MainEvent.NewFrame(frame)
        )

        val userPos = Float3(
            frame.camera.displayOrientedPose.tx(),
            frame.camera.displayOrientedPose.ty(),
            frame.camera.displayOrientedPose.tz()
        )

        if (System.currentTimeMillis() - lastPositionTime > POSITION_DETECT_DELAY){
            lastPositionTime = System.currentTimeMillis()
            changeViewablePath(userPos)
        }

        }


    private fun changeLinkPlacementMode(link: Boolean){
        linkPlacementMode = link
        binding.linkButton.text = if (link) getString(R.string.cancel) else getString(R.string.link)
    }
    
    fun selectNode(node: Node?){
        selectedNode = node

        binding.linkButton.isEnabled = node != null
        binding.deleteButton.isEnabled = node != null
    }

    private fun removeNode(node: Node?){
        val treeNode = treeNodesToModels.inverse[node]
        treeNode?.let {
            mainModel.onEvent(MainEvent.DeleteNode(treeNode))
        }
    }

    private fun linkNodes(node1: Node, node2: Node){
        viewLifecycleOwner.lifecycleScope.launch {
            val path1: TreeNode? = treeNodesToModels.inverse[node1]
            val path2: TreeNode? = treeNodesToModels.inverse[node2]

            if (path1 != null && path2 != null){
                mainModel.onEvent(MainEvent.LinkNodes(path1, path2))
            }
        }
    }

    private fun changeViewablePath(userPosition: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val nodes = currentPathState.path?.getNearNodes(
                number = VIEWABLE_PATH_NODES,
                position = userPosition
            )
            pathAdapter.commit(nodes ?: listOf())
        }

    }

//    private fun reDrawPath(path: Path?, wayNodes: MutableList<ArNode>){
//        wayBuildingJob?.cancel()
//        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch {
//            drawerHelper.drawWay(
//                path,
//                wayNodes,
//                binding.sceneView
//            )
//        }
//    }

    private fun createNode(
        frame: ArFrame? = binding.sceneView.currentFrame,
        number: String? = null,
        position: Float3? = null,
        orientation: Quaternion? = null,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            frame?.let {
                val result = useHitTest(frame).getOrNull()
                if (result != null) {
                    mainModel.onEvent(MainEvent.CreateNode(
                        number,
                        position,
                        orientation,
                        result
                    ))
                }
            }
        }
    }

    private fun useHitTest(
        frame: ArFrame,
        x: Float = frame.session.displayWidth / 2f,
        y: Float = frame.session.displayHeight / 2f,
    ): Result<HitTestResult> {

        return hitTest(frame, Float2(x, y))
    }

    private suspend fun onInitializeSuccess() {
        if (mode == ADMIN_MODE) {
            drawerHelper.drawTree(
                mainModel.tree,
                treeNodesToModels,
                modelsToLinkModels,
                binding.sceneView
            )

            binding.deleteButton.isEnabled = true
            binding.linkButton.isEnabled = true
            binding.entryButton.isEnabled = true
            binding.placeButton.isEnabled = true
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.sceneView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    companion object {
        const val ADMIN_MODE = "ADMIN"
        const val USER_MODE = "USER"

        const val mode = USER_MODE

        const val VIEWABLE_PATH_NODES = 21

        const val POSITION_DETECT_DELAY = 100L

        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
    }
}