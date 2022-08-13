package com.example.festunavigator.presentation.confirmer

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
import com.example.festunavigator.databinding.FragmentConfirmBinding
import com.example.festunavigator.presentation.preview.MainEvent
import com.example.festunavigator.presentation.preview.MainShareModel
import com.example.festunavigator.presentation.preview.MainUiEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ConfirmFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()

    private var _binding: FragmentConfirmBinding? = null
    private val binding get() = _binding!!

    private val args: ConfirmFragmentArgs by navArgs()
    private val confType by lazy { args.confirmType }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mainModel.onEvent(MainEvent.RejectConfObject(confType))
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
            mainModel.onEvent(MainEvent.AcceptConfObject(confType))
        }

        binding.rejectButton.setOnClickListener {
            setEnabled(false)
            mainModel.onEvent(MainEvent.RejectConfObject(confType))
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.mainUiEvents.collect { uiEvent ->
                        when (uiEvent) {
                            is MainUiEvent.InitSuccess -> {
                                val action = ConfirmFragmentDirections.actionConfirmFragmentToRouterFragment()
                                findNavController().navigate(action)
                            }
                            is MainUiEvent.InitFailed -> {
                                findNavController().popBackStack()
                            }
                            is MainUiEvent.EntryCreated -> {
                                val action = ConfirmFragmentDirections.actionConfirmFragmentToRouterFragment()
                                findNavController().navigate(action)
                            }
                            is MainUiEvent.EntryAlreadyExists -> {
                                val action = ConfirmFragmentDirections.actionConfirmFragmentToRouterFragment()
                                findNavController().navigate(action)
                            }
                            else -> {}
                        }
                }
            }
        }

    }

    private fun setEnabled(enabled: Boolean) {
        binding.acceptButton.isEnabled = enabled
        binding.rejectButton.isEnabled = enabled

    }

    companion object {
        const val CONFIRM_INITIALIZE = 0
        const val CONFIRM_ENTRY = 1
    }

}