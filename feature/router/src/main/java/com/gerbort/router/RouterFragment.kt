package com.gerbort.router

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gerbort.common.model_ext.getEntryLocation
import com.gerbort.core_ui.frame_holder.FrameProducer
import com.gerbort.hit_test.HitTestResult
import com.gerbort.hit_test.HitTestUseCase
import com.gerbort.pathfinding.domain.manager.PathManager
import com.gerbort.router.databinding.FragmentRouterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RouterFragment: Fragment() {

    @Inject
    lateinit var hitTest: HitTestUseCase
    @Inject
    lateinit var frameProducer: FrameProducer
    @Inject
    lateinit var pathManager: PathManager

    private var _binding: FragmentRouterBinding? = null
    private val binding get() = _binding!!

    private val vm: RouterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (BuildCon.mode == App.ADMIN_MODE) {
            binding.adminPanel.isVisible = true
        } else {
            binding.adminPanel.isGone = true
        }

        binding.deleteButton.setOnClickListener {
            removeSelectedNode()
        }

        binding.linkButton.setOnClickListener {
            changeLinkPlacementMode()
        }

        binding.placeButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                createNode()
            }
        }

        binding.entryButton.setOnClickListener {
            //entry = 1 init = 0
            val uri = Uri.parse("android-app://com.gerbort.app/scanner_fragment/1")
            findNavController().navigate(uri)
        }

        binding.fromInput.setOnFocusChangeListener { _, b ->
            if (b) {
                binding.fromInput.isActivated = false
                binding.fromInput.clearFocus()
                search(SearchFragment.TYPE_START)
            }
        }

        binding.toInput.setOnFocusChangeListener { _, b ->
            if (b) {
                binding.toInput.isActivated = false
                binding.toInput.clearFocus()
                search(SearchFragment.TYPE_END)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pathManager.getPathState().collect { pathState ->
                    binding.fromInput.setText(pathState.startEntry?.number ?: "")
                    binding.toInput.setText(pathState.endEntry?.number ?: "")
                    if (pathState.path != null) {
                        binding.destinationText.isVisible = true
                        binding.destinationText.text =
                            resources.getString(
                                R.string.going,
                                pathState.endEntry!!.number.getEntryLocation(requireContext())
                            )
                    } else {
                        binding.destinationText.isGone = true
                    }

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { state ->

                    binding.linkButton.isEnabled = state.selectedNode != null
                    binding.deleteButton.isEnabled = state.selectedNode != null

                    binding.linkButton.text =
                        if (state.linkPlacement) getString(R.string.cancel) else getString(R.string.link)

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiEvents.collect { uiEvent ->
                    when (uiEvent) {
                        is RouterUiEvent.NodeCreated -> {
                            showSnackbar(getString(R.string.node_created))
                        }
                        is RouterUiEvent.LinkCreated -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                //treeAdapter.createLink(uiEvent.node1, uiEvent.node2)
                                showSnackbar(getString(R.string.link_created))
                            }
                        }
                        is RouterUiEvent.NodeDeleted -> {
                            showSnackbar(getString(R.string.node_deleted))
                        }
                        is RouterUiEvent.LinkAlreadyExists -> {
                            showSnackbar(getString(R.string.link_already_exists))
                        }
                        is RouterUiEvent.NodeAlreadyExists -> {
                            showSnackbar(getString(R.string.node_already_exists))
                        }
                    }

                }
            }
        }
    }

    private fun search(type: Int){
        val action = RouterFragmentDirections.actionRouterFragmentToSearchFragment(type)
        findNavController().navigate(action)
    }

    private fun changeLinkPlacementMode(){
        vm.onEvent(RouterEvent.ChangeLinkMode)
    }

    private fun removeSelectedNode(){
        vm.state.value.selectedNode?.let {
            vm.onEvent(RouterEvent.DeleteNode(it))
        }
    }

    private fun createNode(
        position: Float3? = null,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            frameProducer.getFrames().first()?.let { frame ->
                    val result = useHitTest(frame).getOrNull()
                    if (result != null) {
                        vm.onEvent(
                            RouterEvent.CreatePathNode(
                                position ?: result.orientatedPosition.position,
                            ))
                    }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.fromInput, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun useHitTest(
        frame: ArFrame,
        x: Float = frame.session.displayWidth / 2f,
        y: Float = frame.session.displayHeight / 2f,
    ): Result<HitTestResult> {

        return hitTest(frame, Float2(x, y))
    }

}