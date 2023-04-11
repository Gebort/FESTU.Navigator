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
import com.example.festunavigator.databinding.FragmentPreviewBinding
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
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PreviewFragment : Fragment() {

    //an instance of MainShareModel, which is a view model shared between several fragments and activities
    private val mainModel: MainShareModel by activityViewModels()

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val drawerHelper = DrawerHelper(this)

    // instances of Job that are used to manage coroutines that are started and stopped when certain events occur.
    private var endPlacingJob: Job? = null
    private var startPlacingJob: Job? = null
    private var wayBuildingJob: Job? = null
    private var treeBuildingJob: Job? = null

    //an instance of MainShareModel, which is a view model shared between several fragments and activities
    private var currentPathState: PathState? = null
    private var lastPositionTime = 0L

    // An instance of MainShareModel, which is a view model shared between several fragments and activities
    private lateinit var pathAdapter: PathAdapter

    // An instance of TreeAdapter, which is an adapter for displaying the tree of objects in the scene.
    private lateinit var treeAdapter: TreeAdapter

    // an instance of LabelObject, which is a data class that represents a label for an object in the scene
    private var lastConfObject: LabelObject? = null

    // an instance of LabelObject, which is a data class that represents a label for an object in the scene
    private var confObjectJob: Job? = null


    // an instance of MutableBiMap<TreeNode, ArNode>, which is a bidirectional map that maps TreeNode objects to ArNode objects.
    private val treeNodesToModels: MutableBiMap<TreeNode, ArNode> = mutableBiMapOf()
    private var selectionJob: Job? = null
    private var selectionNode: ArNode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)

        return binding.root
    }


    //This method is called when the fragment is resumed. It resumes the AR session (SceneView session)
    override fun onResume() {
        super.onResume()
        binding.sceneView.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.onPause(this)
    }



    // This method is called when the fragment's view is created. It initializes the PathAdapter and TreeAdapter,
// and sets up event listeners for tapping on nodes in the scene.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pathAdapter = PathAdapter(
            drawerHelper,
            binding.sceneView,
            VIEWABLE_PATH_NODES,
            viewLifecycleOwner.lifecycleScope
        )

        treeAdapter = TreeAdapter(
            drawerHelper,
            binding.sceneView,
            DEFAULT_BUFFER_SIZE,
            viewLifecycleOwner.lifecycleScope
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

            //@sahar change : Linking
//The onTap callback is called whenever the user taps the screen. In this case, it checks whether
// the app is in ADMIN_MODE and then performs some actions on the tapped node.
            onTap = { node, _, _ ->
                if (App.mode == App.ADMIN_MODE) {
                    node?.let { it ->
                        //If the linkPlacementMode is off, it selects the node
                        if (!mainModel.linkPlacementMode.value) {
                            selectNode(it as ArNode)
                        }
                        //otherwise it links the selected node with the tapped node.
                        else {
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


        //TAKEOFF
        selectNode(null)

        //This code block is creating and launching several coroutines using the viewLifecycleOwner.
        // lifecycleScope.launch function. The viewLifecycleOwner is a LifecycleOwner that represents
        // the View associated with the current Fragment, and lifecycleScope is a CoroutineScope
        // associated with the lifecycle of the View.

        //ps : coroutines as “lightweight threads”. They are sort of tasks that the actual threads can execute.


        //1.
       // The first coroutine is launched to observe the pathState LiveData object in the mainModel
       // ViewModel. Whenever the pathState value changes, the coroutine updates the 3D scene by
       // removing the old start and end nodes and adding new ones.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.pathState.collectLatest { pathState ->
                    //In ADMIN_MODE, all entry labels are drawn automatically, so another redraw from this function
                    //will cause node
                    //   if (App.mode == App.USER_MODE) {

                    if (currentPathState?.endEntry != pathState.endEntry) {
                        endPlacingJob?.cancel()
                        currentPathState?.endEntry?.let { end ->
                            treeNodesToModels[end]?.let {
                                drawerHelper.removeNode(it)
                            }
                        }
                        endPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            pathState.endEntry?.let { end ->
                                treeNodesToModels[end] = drawerHelper.drawNode(
                                    end,
                                    binding.sceneView,
                                )
                            }
                        }
                    }
                    if (currentPathState?.startEntry != pathState.startEntry) {
                        startPlacingJob?.cancel()
                        currentPathState?.startEntry?.let { start ->
                            treeNodesToModels[start]?.let {
                                drawerHelper.removeNode(it)
                            }
                        }
                        startPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                            pathState.startEntry?.let { start ->
                                treeNodesToModels[start] = drawerHelper.drawNode(
                                    start,
                                    binding.sceneView,
                                )
                            }
                        }
                    }
                    //         }
                    currentPathState = pathState
                }
            }
        }

        //2.
        //The second coroutine is launched to observe the mainUiEvents LiveData object in the mainModel
        // ViewModel. Whenever a MainUiEvent object is emitted, the coroutine updates the UI accordingly,
        // such as showing a snackbar with a message.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.mainUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is MainUiEvent.InitSuccess -> {
                            // onInitializeSuccess()
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


        //3.
        //The third coroutine is launched to observe the confirmationObject LiveData object in the
        // mainModel ViewModel. Whenever a ConfirmationObject object is emitted, the coroutine
        // updates the 3D scene by removing the old confirmation node and adding a new one.

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
            if (App.mode == App.ADMIN_MODE) {
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

    //function updates the list of viewable nodes on the path based on the user's current position.
    // It does this by calling the Path.getNearNodes() method of the currentPathState,
    // which returns the nearest VIEWABLE_PATH_NODES
    private fun changeViewablePath(userPosition: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val nodes = currentPathState?.path?.getNearNodes(
                number = VIEWABLE_PATH_NODES,
                position = userPosition
            )
            pathAdapter.commit(nodes ?: listOf())
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

    //this fct checks if a given node is valid, i.e., if it corresponds to a tree node that is
    // currently displayed on the screen. If the node is valid, it returns its corresponding
    // TreeNode object.
    private fun checkTreeNode(node: ArNode?): TreeNode? {
        //User selected entry can be stored in PreviewFragment nodes map,
        // if this node displayed as PathState start or end
        treeNodesToModels.inverse[node]?.let { return it }
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