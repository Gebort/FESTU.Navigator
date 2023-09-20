package com.example.festunavigator.presentation.preview

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.data.utils.reverseConvertPosition
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
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.*
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.log

class PreviewFragment : Fragment(), SensorEventListener {

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

    private lateinit var sensorManager: SensorManager
    private lateinit var gsensor: Sensor
    private lateinit var msensor: Sensor
    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private var azimuth = 0f
    private var currectAzimuth = 0f

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
        sensorManager.registerListener(this, gsensor,
            SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, msensor,
            SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.onPause(this)
        sensorManager.unregisterListener(this)
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

            onTap = { node, _, _ ->
                if (App.mode == App.ADMIN_MODE) {
                    node?.let { it ->
                        if (!mainModel.linkPlacementMode.value) {
                            selectNode(it as ArNode)
                        } else {
                            val treeNode = mainModel.selectedNode.value
                            (treeAdapter.getArNode(treeNode))?.let { node1 ->
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
                    currentPathState = pathState
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.mainUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is MainUiEvent.InitSuccess -> {
                            treeAdapter.changeParentPos(uiEvent.initialEntry?.position)
                            pathAdapter.changeParentPos(uiEvent.initialEntry?.position)
                            uiEvent.initialEntry?.let {
                                pathAnalyzer = PathAnalyzer(debug = { s, w -> launch { withContext(Dispatchers.Main) { }}}) { t ->
                                    mainModel.onEvent(MainEvent.PivotTransform(t))
                                }
//                                pathAnalyzer = PathAnalyzer(debug = { s, w -> launch { withContext(Dispatchers.Main) { debug(s,w) }}}) { t ->
//                                    mainModel.onEvent(MainEvent.PivotTransform(t))
//                                }
                            }
                            binding.sceneView.planeRenderer.isVisible = App.isAdmin
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
                        is MainUiEvent.LinkAlreadyExists -> {
                            showSnackbar(getString(R.string.link_already_exists))
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.selectedNode.collectLatest { selectedNode ->
                    treeAdapter.updateSelection(selectedNode)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.treePivot.collectLatest { pivot ->
                    pivot?.let {
                        treeAdapter.changeParentPos(it.position, it.orientation)
                        pathAdapter.changeParentPos(it.position, it.orientation)
                    }
                }
            }
        }

        sensorManager = requireActivity()
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
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
            if (App.isAdmin) {
                mainModel.selectedNode.value?.let { node ->
                    checkSelectedNode(node)
                }
            }
        }
    }
    
    private fun selectNode(node: ArNode?){
        val treeNode = checkTreeNode(node) ?: checkTreeNode(node?.parentNode as ArNode?)
        mainModel.onEvent(MainEvent.NewSelectedNode(treeNode))
    }

    private fun checkSelectedNode(treeNode: TreeNode){
        if (treeAdapter.getArNode(treeNode) == null) {
            selectNode(null)
        }
    }

    private fun linkNodes(node1: ArNode, node2: ArNode){
        viewLifecycleOwner.lifecycleScope.launch {
            val path1: TreeNode? = checkTreeNode(node1) ?: checkTreeNode(node1.parentNode as ArNode)
            val path2: TreeNode? = checkTreeNode(node2) ?: checkTreeNode(node2.parentNode as ArNode)

            if (path1 != null && path2 != null){
                mainModel.onEvent(MainEvent.LinkNodes(path1, path2))
            }
        }
    }

    private fun changeViewablePath(userPositionTrans: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val nodes = currentPathState?.path?.getNearNodes(
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
                pathAdapter.getPivot()?.orientation?.let { parentQ ->
                    val pathSegment = mainModel.treeDiffUtils.getClosestSegment(userPositionTrans)
                    pathAnalyzer?.newPosition(
                        userPositionReal,
                        pathSegment,
                        parentQ
                    )
                }
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
        //image crop for recognition
        val DESIRED_CROP = Pair(8, 72)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
                // mGravity = event.values;
//                Log.d(TAG, "GravityRaw: "+event.values.contentToString()+ " filtered: "+mGravity.contentToString());
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // mGeomagnetic = event.values;
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
//                Log.d(TAG,"MagneticRaw: "+event.values.contentToString()+ " Filtered: "+mGeomagnetic.contentToString());
            }

            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)
//            Log.d(TAG,"Gravity: "+mGravity.contentToString()+ " Magnetic: "+mGeomagnetic.contentToString()
//            + " R: "+ R.contentToString() +" I: "+ I.contentToString());
            if (success) {
//                Log.d(TAG,"Gravity: "+mGravity.contentToString()+ " Magnetic: "+mGeomagnetic.contentToString())
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                // Log.d(TAG, "azimuth (rad): " + azimuth);
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // orientation
                azimuth = (azimuth + 360) % 360
                debug(String.format("%.0f", azimuth), 1)

                mainModel.onEvent(MainEvent.NewAzimuth(orientation[0]))
            }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }
}