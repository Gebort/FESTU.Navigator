package com.example.festunavigator.presentation.preview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.festunavigator.presentation.preview.nodes_adapters.PathAdapter
import com.example.festunavigator.presentation.preview.nodes_adapters.TreeAdapter
import com.gerbort.app.R
import com.gerbort.app.databinding.FragmentPreviewBinding
import com.gerbort.common.model.TreeNode
import com.gerbort.common.utils.IS_ADMIN_MODE
import com.gerbort.common.utils.IS_USER_MODE
import com.gerbort.common.utils.reverseConvertPosition
import com.gerbort.core_ui.drawer_helper.DrawerHelper
import com.gerbort.core_ui.frame_holder.FrameConsumer
import com.gerbort.core_ui.tap_flow.UserTap
import com.gerbort.core_ui.tap_flow.UserTapConsumer
import com.gerbort.node_graph.domain.graph.SingleLinksChangeListener
import com.gerbort.path_correction.domain.PathCorrector
import com.gerbort.pathfinding.domain.manager.PathManager
import com.gerbort.sensor_handling.SensorHandler
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()
    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private var wayBuildingJob: Job? = null
    private var treeBuildingJob: Job? = null
    private var lastPositionTime = 0L

    private lateinit var pathAdapter: PathAdapter
    private lateinit var treeAdapter: TreeAdapter

    @Inject lateinit var sensorHandler: SensorHandler
    @Inject lateinit var pathManager: PathManager
    @Inject lateinit var pathCorrector: PathCorrector
    @Inject lateinit var frameConsumer: FrameConsumer
    @Inject lateinit var userTapConsumer: UserTapConsumer
    @Inject lateinit var drawerHelper: DrawerHelper

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
        sensorHandler.register(requireActivity())
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.onPause(this)
        sensorHandler.unregister()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        drawerHelper.setFragment(this)
        drawerHelper.setParentNode(binding.sceneView)

        pathAdapter = PathAdapter(
            drawerHelper = drawerHelper,
            previewView = binding.sceneView,
            bufferSize = VIEWABLE_PATH_NODES,
            scope = viewLifecycleOwner.lifecycleScope,
        )

        treeAdapter = TreeAdapter(
            drawerHelper = drawerHelper,
            previewView = binding.sceneView,
            bufferSize = DEFAULT_BUFFER_SIZE,
            scope = viewLifecycleOwner.lifecycleScope,
            onlyEntries = IS_USER_MODE
        )
        mainModel.setSingleLinksChangeListener(object : SingleLinksChangeListener {
            override fun onLinkAdded(nodeStart: TreeNode, nodeEnd: TreeNode) {
                treeAdapter.newLinkAdded(nodeStart, nodeEnd)
            }
        })

        binding.sceneView.apply {
            planeRenderer.isVisible = true
            instructions.enabled = false
            onArFrame = { frame ->
                onDrawFrame(frame)
            }
            configureSession { _, config ->
                config.depthMode = Config.DepthMode.AUTOMATIC
                config.focusMode = Config.FocusMode.AUTO
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            }

            onTap = { node, renderable, event ->
                node?.let {
                    val treeNode = checkTreeNode(it as ArNode) ?: checkTreeNode(node.parentNode as ArNode?)
                    Log.d("testing", "tapped node ${treeNode?.id}, number: ${(treeNode as? TreeNode.Entry)?.number}")
                    userTapConsumer.newTap(UserTap(
                        node = node,
                        treeNode = treeNode,
                        renderable = renderable,
                        motionEvent = event,
                    ))
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.uiEvent.collect { uiEvent ->
                    when (uiEvent) {
                        is MainUiEvent.GraphPositionChanged -> {
                            treeAdapter.changeParentPos(uiEvent.nodeGraphPosition.pivotPosition)
                            pathAdapter.changeParentPos(uiEvent.nodeGraphPosition.pivotPosition)
                            uiEvent.nodeGraphPosition.let {
//                                pathAnalyzer = PathAnalyzer(debug = { s, w -> launch { withContext(Dispatchers.Main) { }}}) { t ->
//                                    mainModel.onEvent(MainEvent.PivotTransform(t))
//                                }
//                                pathAnalyzer = PathAnalyzer(debug = { s, w -> launch { withContext(Dispatchers.Main) { debug(s,w) }}}) { t ->
//                                    mainModel.onEvent(MainEvent.PivotTransform(t))
//                                }
                            }
                            binding.sceneView.planeRenderer.isVisible = IS_ADMIN_MODE
                        }
                    }
                }
            }
        }
       // sensorHandler.startHandling(requireContext())
    }


    private fun onDrawFrame(frame: ArFrame) {

        val camera = frame.camera

        // Handle tracking failures.
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

        frameConsumer.newFrame(frame)

        val userPosReal = Float3(
            frame.camera.displayOrientedPose.tx(),
            frame.camera.displayOrientedPose.ty(),
            frame.camera.displayOrientedPose.tz()
        )
        //we need to find the translocated position, because of possible orientation correction
        val userPosTrans = treeAdapter.getPivot()?.let { pn ->
            pn.orientation.reverseConvertPosition(
                position = userPosReal,
                pivotPosition = pn.position,
            )
        } ?: userPosReal.copy()

        if (System.currentTimeMillis() - lastPositionTime > POSITION_DETECT_DELAY){
            lastPositionTime = System.currentTimeMillis()
            changeViewablePath(userPosTrans)
            changeViewableTree(userPosReal, userPosTrans)
//            if (App.isAdmin) {
//                mainModel.selectedNode.value?.let { node ->
//                    checkSelectedNode(node)
//                }
//            }
        }
    }

    private fun changeViewablePath(userPositionTrans: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {

            //TODO move changeViewablePath to separate flow and combine it with pathManager.getPathState()
            val nodes = pathManager.getPathState().first().pathDiffUtils?.getNearNodes(
                number = VIEWABLE_PATH_NODES,
                position = userPositionTrans
            ) ?: listOf()
            pathAdapter.commit(nodes)
        }
    }

    private fun changeViewableTree(userPositionReal: Float3, userPositionTrans: Float3){
        if (treeBuildingJob?.isCompleted == true || treeBuildingJob?.isCancelled == true || treeBuildingJob == null) {
            treeBuildingJob?.cancel()
            treeBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val nodes = mainModel.treeDiffUtils.getNearNodes(
                    radius = VIEWABLE_ADMIN_NODES_DISTANCE,
                    position = userPositionTrans
                )

                treeAdapter.commit(nodes)
//                pathAdapter.getPivot()?.orientation?.let { parentQ ->
//                    val pathSegment = mainModel.treeDiffUtils.getClosestSegment(userPositionTrans)
//                    pathAnalyzer?.newPosition(
//                        userPositionReal,
//                        pathSegment,
//                        parentQ
//                    )
                }
            }

    }

    private fun checkTreeNode(arNode: ArNode?): TreeNode? = treeAdapter.getTreeNode(arNode)

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.sceneView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    fun debug (text: String, which: Int) {
        when (which) {
            1 -> binding.textDebug.text = text
            2 -> binding.textDebug2.text = text
        }
    }

    companion object {
        //how many path nodes will be displayed at the moment
        const val VIEWABLE_PATH_NODES = 31
        //distance of viewable nodes for admin mode
        const val VIEWABLE_ADMIN_NODES_DISTANCE = 8f
        //how often the check for path and tree redraw will be
        const val POSITION_DETECT_DELAY = 100L
    }
}