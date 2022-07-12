package com.example.festunavigator.presentation.router

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
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.databinding.FragmentRouterBinding
import com.example.festunavigator.databinding.FragmentSearchBinding
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.preview.MainShareModel
import com.example.festunavigator.presentation.search.SearchFragment
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RouterFragment : Fragment() {

    private val destinationDesc = App.instance!!.getDestinationDesc

    private val mainModel: MainShareModel by activityViewModels()

    private var _binding: FragmentRouterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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
                mainModel.pathState.collect { pathState ->
                    binding.fromInput.setText(pathState.startLabel?.label ?: "")
                    binding.toInput.setText(pathState.endLabel?.label ?: "")
                    if (pathState.path != null){
                        binding.destinationText.isVisible = true
                        binding.destinationText.text =
                            resources.getString(
                                R.string.going,
                                destinationDesc(pathState.endLabel!!.label, requireContext())
                            )
                    }
                    else {
                        binding.destinationText.isGone = true
                    }

                }
            }
        }
    }

    private fun search(type: Int){
        val action = RouterFragmentDirections.actionRouterFragmentToSearchFragment(type)
        findNavController().navigate(action)
    }

}