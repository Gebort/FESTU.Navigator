package com.example.festunavigator.presentation.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.festunavigator.R
import com.example.festunavigator.data.App
import com.example.festunavigator.databinding.FragmentPreviewBinding
import com.example.festunavigator.databinding.FragmentSearchBinding
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.common.adapters.EntriesAdapter
import com.example.festunavigator.presentation.common.adapters.EntryItem
import com.example.festunavigator.presentation.common.helpers.viewHideInput
import com.example.festunavigator.presentation.common.helpers.viewRequestInput
import com.example.festunavigator.presentation.preview.MainEvent
import com.example.festunavigator.presentation.preview.MainShareModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchFragment : Fragment() {

    private val destinationDesc = App.instance!!.getDestinationDesc

    private val mainModel: MainShareModel by activityViewModels()

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val adapter = EntriesAdapter(){ number ->
         processSearchResult(number)
    }

    private val args: SearchFragmentArgs by navArgs()
    val changeType by lazy { args.changeType }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        with(binding.searchInput){
            doOnTextChanged { text, _, _, _ ->
                adapter.applyFilter(text.toString())
            }
            setOnEditorActionListener { v, actionId, event ->
                var handled = false
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    processSearchResult(text.toString())
                    handled = true
                }
                handled
            }
            binding.searchLayout.hint =
                if (changeType == TYPE_END) getString(R.string.to)
                else getString(R.string.from)
        }

        binding.entryRecyclerView.adapter = adapter
        binding.entryRecyclerView.layoutManager = LinearLayoutManager(
                requireActivity().applicationContext
        )

        var entriesList = listOf<EntryItem>()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO){
                entriesList = mainModel.tree.getEntriesNumbers().map { number ->
                    EntryItem(number, destinationDesc(number, requireActivity().applicationContext))
                }
            }
            adapter.changeList(entriesList)
        }

        viewRequestInput(
            binding.searchInput,
            requireContext()
        )

//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                mainModel.pathState.collectLatest { path ->
//                    //TODO обработка начальной и конечной точки
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.searchUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is SearchUiEvent.SearchSuccess -> {
                            binding.searchLayout.error = null
                            findNavController().popBackStack()
                        }
                        is SearchUiEvent.SearchInvalid -> {
                            binding.searchLayout.error = resources.getString(R.string.incorrect_number)
                            viewHideInput(
                                binding.searchInput,
                                requireContext()
                            )
                            viewRequestInput(
                                binding.searchInput,
                                requireContext()
                            )
                        }
                    }
                }
                }
            }
    }

    private fun processSearchResult(number: String) {
        mainModel.onEvent(MainEvent.TrySearch(number, changeType))
    }

    companion object {
        const val TYPE_START = 0
        const val TYPE_END = 1
    }

}