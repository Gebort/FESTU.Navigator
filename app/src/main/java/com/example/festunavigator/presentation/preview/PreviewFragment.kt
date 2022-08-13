package com.example.festunavigator.presentation.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.databinding.FragmentPreviewBinding
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.example.festunavigator.presentation.preview.path_adapter.PathAdapter
import com.example.festunavigator.presentation.preview.state.PathState
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.*
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PreviewFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()


    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val drawerHelper = DrawerHelper(this)

    private var endPlacingJob: Job? = null
    private var startPlacingJob: Job? = null
    private var wayBuildingJob: Job? = null
    private var currentPathState = PathState()
    private var lastPositionTime = 0L
    private lateinit var pathAdapter: PathAdapter


    private var lastConfObject: LabelObject? = null
    private var confObjectJob: Job? = null
    private val treeNodesToModels: MutableBiMap<TreeNode, ArNode> = mutableBiMapOf()
    private val modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode> = mutableBiMapOf()

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

        //TODO process pause and resume
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
            onTouch = {node, _ ->
                if (App.mode == App.ADMIN_MODE) {
                    node?.let { it ->
                        if (!mainModel.linkPlacementMode.value) {
                            selectNode(it)
                        } else {
                            val treeNode = mainModel.selectedNode.value
                            treeNodesToModels[treeNode]?.let { node1 ->
                                linkNodes(node1, it)
                            }
                        }
                    }
                }
                true
            }
//            onTouchEvent = { pickHitResult, _ ->
//                if (App.mode == App.ADMIN_MODE) {
//                    pickHitResult.node?.let { node ->
//                        if (!mainModel.linkPlacementMode.value) {
//                            selectNode(node)
//                        } else {
//                            val treeNode = mainModel.selectedNode.value
//                            treeNodesToModels[treeNode]?.let { node1 ->
//                                linkNodes(node1, node)
//                            }
//                        }
//                    }
//                }
//                true
//            }
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

        selectNode(null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.pathState.collectLatest { pathState ->
                    if (currentPathState.path != pathState.path) {
                        //reDrawPath(pathState.path, wayNodes)
                    }
                    if (currentPathState.endEntry != pathState.endEntry){
                        endPlacingJob?.cancel()
                        currentPathState.endEntry?.let { entry ->
                            treeNodesToModels[entry]?.let {
                                drawerHelper.removeLabelWithAnim(it)
                            }
                        }

                        endPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            currentPathState.endEntry?.let { end ->
                                treeNodesToModels[end] = drawerHelper.placeLabel(
                                    end.number,
                                    OrientatedPosition(end.position, end.forwardVector),
                                    binding.sceneView,
                                )
                            }
                        }
                    }
                    if (currentPathState.startEntry != pathState.startEntry){
                        startPlacingJob?.cancel()
                        currentPathState.startEntry?.let { entry ->
                            treeNodesToModels[entry]?.let {
                                drawerHelper.removeLabelWithAnim(it)
                            }
                        }
                        startPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            currentPathState.startEntry?.let { start ->
                                treeNodesToModels[start] = drawerHelper.placeLabel(
                                    start.number,
                                    OrientatedPosition(start.position, start.forwardVector),
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
                        is MainUiEvent.EntryAlreadyExists -> {
                            showSnackbar(getString(R.string.entry_already_exists))
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
                            drawerHelper.removeNode(it)
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
    
    private fun selectNode(node: Node?){
        mainModel.onEvent(MainEvent.NewSelectedNode(treeNodesToModels.inverse[node]))
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



    private suspend fun onInitializeSuccess() {
        if (App.mode == App.ADMIN_MODE) {
            drawerHelper.drawTree(
                mainModel.tree,
                treeNodesToModels,
                modelsToLinkModels,
                binding.sceneView
            )
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.sceneView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    companion object {
        //how many path nodes will be displayed at the moment
        const val VIEWABLE_PATH_NODES = 21
        const val POSITION_DETECT_DELAY = 100L
        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
    }
}