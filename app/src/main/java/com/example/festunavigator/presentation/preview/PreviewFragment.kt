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
import androidx.navigation.findNavController
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.data.utils.convertPosition
import com.example.festunavigator.databinding.FragmentPreviewBinding
import com.example.festunavigator.domain.pathfinding.path_restoring.PathAnalyzer
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.tree.WrongEntryException
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.example.festunavigator.presentation.preview.nodes_adapters.PathAdapter
import com.example.festunavigator.presentation.preview.nodes_adapters.TreeAdapter
import com.example.festunavigator.presentation.preview.state.PathState
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.*
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.toRotation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

class PreviewFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val drawerHelper = DrawerHelper(this)

    private var wayBuildingJob: Job? = null
    private var treeBuildingJob: Job? = null
    private var currentPathState: PathState? = null
    private var lastPositionTime = 0L
    private lateinit var pathAdapter: PathAdapter
    private lateinit var treeAdapter: TreeAdapter
    private var pathAnalyzer: PathAnalyzer? = null


    private var lastConfObject: LabelObject? = null
    private var confObjectJob: Job? = null
    private var selectionJob: Job? = null
    private var selectionNode: ArNode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        pathAdapter = PathAdapter(
            drawerHelper,
            binding.sceneView,
            VIEWABLE_PATH_NODES,
            viewLifecycleOwner.lifecycleScope,
        )

        treeAdapter = TreeAdapter(
            drawerHelper,
            binding.sceneView,
            DEFAULT_BUFFER_SIZE,
            viewLifecycleOwner.lifecycleScope,
            onlyEntries = App.isUser
        )

        binding.sceneView.apply {
            planeRenderer.isVisible = App.mode == App.ADMIN_MODE
            instructions.enabled = false
            onArFrame = { frame ->
                onDrawFrame(frame)
            }
            configureSession { arSession, config ->
                config.depthMode = Config.DepthMode.AUTOMATIC
                config.focusMode = Config.FocusMode.AUTO
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            }

            onTap = { node, _, _ ->
                if (App.mode == App.ADMIN_MODE) {
                    node?.let { it ->
                        if (!mainModel.linkPlacementMode.value) {
                            selectNode(it as ArNode)
                        } else {
                            val treeNode = mainModel.selectedNode.value
                            treeAdapter.getArNode(treeNode)?.let { node1 ->
                                linkNodes(node1, it as ArNode)
                            }
                        }
                    }
                }
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

        selectNode(null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.pathState.collectLatest { pathState ->
                    if (pathState.startEntry != null && pathState.endEntry != null) {
                        pathAdapter.changeParentPos(pathState.startEntry.position)
                        treeAdapter.changeParentPos(pathState.startEntry.position)
                        //TODO мы не учитываем ситуацию, когда человек просканировал точку инициализации с ошибкой,
                        //TODO а после сменил начальную точку маршрута. тогда простой поворот не поможет, нужен еще перенос
                        pathAnalyzer = PathAnalyzer(debug = { s -> launch { withContext(Dispatchers.Main) { binding.textDebug.text = s }}}) {
                            pathAdapter.changeParentPos(transition = it)
                            treeAdapter.changeParentPos(transition = it)
                        }
                    }
                    else {
                        pathAnalyzer = null
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
                            treeAdapter.changeParentPos(Float3(0f))
                        }
                        is MainUiEvent.InitFailed -> {
                            when (uiEvent.error) {
                                is WrongEntryException -> {
                                    showSnackbar(getString(R.string.incorrect_number))
                                }
                                else -> {
                                    showSnackbar(getString(R.string.init_failed))
                                }
                            }
                        }
                        is MainUiEvent.NodeCreated -> {
                            showSnackbar(getString(R.string.node_created))
                        }
                        is MainUiEvent.LinkCreated -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                treeAdapter.createLink(uiEvent.node1, uiEvent.node2)
                                showSnackbar(getString(R.string.link_created))
                            }
                        }
                        is MainUiEvent.NodeDeleted -> {
                            showSnackbar(getString(R.string.node_deleted))
                        }
                        is MainUiEvent.PathNotFound -> {
                            showSnackbar(getString(R.string.no_path))
                        }
                        is MainUiEvent.EntryAlreadyExists -> {
                            showSnackbar(getString(R.string.entry_already_exists))
                        }
                        else -> {}
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
            if (App.isAdmin) {
                changeViewableTree(userPos)
                mainModel.selectedNode.value?.let { node ->
                    checkSelectedNode(node)
                }
            }
        }
    }
    
    private fun selectNode(node: ArNode?){
        val treeNode = checkTreeNode(node) ?: checkTreeNode(node?.parentNode as ArNode?)

        selectionJob?.cancel()
        selectionNode?.let { drawerHelper.removeNode(it) }
        treeNode?.let {
            selectionJob = viewLifecycleOwner.lifecycleScope.launch {
                selectionNode = drawerHelper.drawSelection(it, binding.sceneView)
            }
        }
        mainModel.onEvent(MainEvent.NewSelectedNode(treeNode))
    }

    private fun checkSelectedNode(treeNode: TreeNode){
        if (treeAdapter.getArNode(treeNode) == null) {
            selectNode(null)
        }
    }

    private fun linkNodes(node1: ArNode, node2: ArNode){
        viewLifecycleOwner.lifecycleScope.launch {
            val path1: TreeNode? = treeAdapter.getTreeNode(node1)
            val path2: TreeNode? = treeAdapter.getTreeNode(node2)

            if (path1 != null && path2 != null){
                mainModel.onEvent(MainEvent.LinkNodes(path1, path2))
            }
        }
    }

    private fun changeViewablePath(userPosition: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val nodes = currentPathState?.path?.getNearNodes(
                number = VIEWABLE_PATH_NODES,
                position = userPosition
            ) ?: listOf()
            pathAdapter.commit(nodes)
            //TODO постоянно считаем направление всех узлов пути. нужно отслеживать направление только новых
            pathAdapter.parentNode?.quaternion?.let { parentQ ->
                pathAnalyzer?.newPosition(
                    userPosition,
                    nodes.drop(nodes.size/2-2)
                        .dropLast(nodes.size/2-2),
                    parentQ
                )
            }
        }
    }

    //ONLY FOR ADMIN MODE
    private fun changeViewableTree(userPosition: Float3){
        if (treeBuildingJob?.isCompleted == true || treeBuildingJob?.isCancelled == true || treeBuildingJob == null) {
            treeBuildingJob?.cancel()
            treeBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val nodes = mainModel.treeDiffUtils.getNearNodes(
                    radius = VIEWABLE_ADMIN_NODES,
                    position = userPosition
                )
                treeAdapter.commit(nodes)
            }
        }

    }

    private fun checkTreeNode(node: ArNode?): TreeNode? {
        treeAdapter.getTreeNode(node)?.let { return it }
        return null
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.sceneView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    companion object {
        //how many path nodes will be displayed at the moment
        const val VIEWABLE_PATH_NODES = 31
        //distance of viewable nodes for admin mode
        const val VIEWABLE_ADMIN_NODES = 8f
        //how often the check for path and tree redraw will be
        const val POSITION_DETECT_DELAY = 100L
        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
    }
}