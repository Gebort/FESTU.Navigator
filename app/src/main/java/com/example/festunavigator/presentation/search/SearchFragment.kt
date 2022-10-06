package com.example.festunavigator.presentation.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isVisible
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
import com.example.festunavigator.databinding.FragmentSearchBinding
import com.example.festunavigator.domain.use_cases.GetDestinationDesc
import com.example.festunavigator.presentation.search.adapters.EntriesAdapter
import com.example.festunavigator.presentation.search.adapters.EntryItem
import com.example.festunavigator.presentation.common.helpers.viewHideInput
import com.example.festunavigator.presentation.common.helpers.viewRequestInput
import com.example.festunavigator.presentation.preview.MainEvent
import com.example.festunavigator.presentation.preview.MainShareModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment: Fragment() {

    private val mainModel: MainShareModel by activityViewModels()

    @Inject
    lateinit var destinationDesc: GetDestinationDesc

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val adapter = EntriesAdapter(
        onItemClick = { number -> processSearchResult(number) },
        onEmptyList = { binding.textEmpty.isVisible = true },
        onNotEmptyList = { binding.textEmpty.isGone = true }
    )

    private val args: SearchFragmentArgs by navArgs()
    private val changeType by lazy { args.changeType }

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

        binding.entryRecyclerView.let {
            it.adapter = adapter
            it.itemAnimator = null
            it.layoutManager = LinearLayoutManager(
                requireActivity().applicationContext
            )
        }


        var entriesList = listOf<EntryItem>()
        mainModel.onEvent(MainEvent.LoadRecords)

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                entriesList = mainModel.entriesNumber
                    .map { number ->
                        EntryItem(
                            number,
                            destinationDesc(number, requireActivity().applicationContext)
                        )
                    }
            }
            adapter.changeList(entriesList)

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                mainModel.timeRecords.collectLatest { records ->
                    adapter.changeHistory(
                        records.map {
                            EntryItem(
                                it.end,
                                destinationDesc(it.end, requireActivity().applicationContext),
                                true
                            )
                        }
                    )
                }

            }
        }

        binding.searchInput.viewRequestInput()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.searchUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is SearchUiEvent.SearchSuccess -> {
                            binding.searchLayout.error = null
                            binding.searchInput.viewHideInput()
                            findNavController().popBackStack()
                        }
                        is SearchUiEvent.SearchInvalid -> {
                            binding.searchLayout.error = resources.getString(R.string.incorrect_number)
                            binding.searchInput.viewRequestInput()
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