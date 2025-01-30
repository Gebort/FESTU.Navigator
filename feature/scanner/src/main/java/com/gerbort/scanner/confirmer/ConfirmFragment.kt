package com.gerbort.scanner.confirmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gerbort.core_ui.drawer_helper.DrawerHelper
import com.gerbort.scanner.LabelObject
import com.gerbort.scanner.R
import com.gerbort.scanner.ScannerEvent
import com.gerbort.scanner.ScannerUiEvents
import com.gerbort.scanner.ScannerViewModel
import com.gerbort.scanner.databinding.FragmentConfirmBinding
import com.gerbort.scanner.navigation.ScannerNavigator
import dagger.hilt.android.AndroidEntryPoint
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmFragment : Fragment() {

    private var _binding: FragmentConfirmBinding? = null
    private val binding get() = _binding!!

    private val vm: ScannerViewModel by activityViewModels()

    private var lastConfObject: LabelObject? = null
    private var confObjectNode: ArNode? = null
    private var confObjectJob: Job? = null

    @Inject lateinit var navigator: ScannerNavigator
    @Inject lateinit var drawerHelper: DrawerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.onEvent(ScannerEvent.RejectObject)
                    navigator.popBackStack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setEnabled(true)

        binding.acceptButton.setOnClickListener {
            setEnabled(false)
            vm.onEvent(ScannerEvent.AcceptObject)
        }

        binding.rejectButton.setOnClickListener {
            setEnabled(false)
            vm.onEvent(ScannerEvent.RejectObject)
            navigator.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                vm.state.collect { state ->
                    if (state.confirmObject != lastConfObject) {
                        lastConfObject = state.confirmObject
                        confObjectJob?.cancel()
                        confObjectJob = viewLifecycleOwner.lifecycleScope.launch {
                            confObjectNode?.let {
                                drawerHelper.removeNode(it)
                            }
                            lastConfObject?.let {
                                confObjectNode = drawerHelper.placeLabel(
                                    it.label,
                                    it.pos,
                                )
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.confirmUiEvents.collect { uiEvent ->
                        when (uiEvent) {
                            is ScannerUiEvents.InitFailed -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.init_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigator.popBackStack()
                            }
                            is ScannerUiEvents.InitSuccess -> onSuccess()
                            is ScannerUiEvents.EntryCreated -> onSuccess()
                            is ScannerUiEvents.EntryAlreadyExists -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.entry_already_exists),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSuccess()
                            }

                        }
                }
            }
        }

    }

    private fun onSuccess() {
        confObjectNode?.let {
            drawerHelper.removeNode(it)
        }
        navigator.navigateOnEntryCreationSuccess()
    }

    private fun setEnabled(enabled: Boolean) {
        binding.acceptButton.isEnabled = enabled
        binding.rejectButton.isEnabled = enabled

    }

}