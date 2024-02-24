package com.gerbort.scanner.confirmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.festunavigator.presentation.preview.MainEvent
import com.example.festunavigator.presentation.preview.MainShareModel
import com.example.festunavigator.presentation.preview.MainUiEvent
import com.gerbort.app.databinding.FragmentConfirmBinding
import com.gerbort.scanner.ScannerEvent
import com.gerbort.scanner.ScannerUiEvents
import com.gerbort.scanner.ScannerViewModel
import com.gerbort.scanner.databinding.FragmentConfirmBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ConfirmFragment : Fragment() {

    private var _binding: FragmentConfirmBinding? = null
    private val binding get() = _binding!!

    private val vm: ScannerViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.onEvent(ScannerEvent.RejectObject)
                    findNavController().popBackStack()
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
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.confirmUiEvents.collect { uiEvent ->
                        when (uiEvent) {
                            is ScannerUiEvents.InitFailed -> {
                                findNavController().popBackStack()
                            }
                            is ScannerUiEvents.InitSuccess -> onSuccess()
                            is ScannerUiEvents.EntryCreated -> onSuccess()
                            is ScannerUiEvents.EntryAlreadyExists -> onSuccess()

                        }
                }
            }
        }

    }

    private fun onSuccess() {
        val action = ConfirmFragmentDirections.actionConfirmFragmentToRouterFragment()
        findNavController().navigate(action)
    }

    private fun setEnabled(enabled: Boolean) {
        binding.acceptButton.isEnabled = enabled
        binding.rejectButton.isEnabled = enabled

    }

}