package com.gerbort.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.festunavigator.presentation.search.adapters.EntryItem
import com.gerbort.common.model_ext.getEntryLocation
import com.gerbort.core_ui.utils.viewHideInput
import com.gerbort.core_ui.utils.viewRequestInput
import com.gerbort.search.adapters.EntriesAdapter
import com.gerbort.search.databinding.FragmentSearchBinding
import com.gerbort.search.navigation.SearchNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment: Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val vm: SearchViewModel by activityViewModels()

    @Inject lateinit var navigator: SearchNavigator

    private val adapter = EntriesAdapter(
        onItemClick = { number -> processSearchResult(number) },
    )

    private val searchType: SearchType by lazy {
        SearchType.fromInt(arguments?.getInt(SEARCH_TYPE) ?: throw IllegalStateException("no searchType"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigator.popBackStack()
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

        vm.onEvent(SearchEvents.LoadRecordsAndEntries)

        with(binding.searchInput){
            doOnTextChanged { text, _, _, _ ->
                    vm.onEvent(SearchEvents.Filter(text.toString()))
            }
            setOnEditorActionListener { v, actionId, event ->
                var handled = false
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    vm.onEvent(SearchEvents.Filter(text.toString()))
                    handled = true
                }
                handled
            }
            binding.searchLayout.hint =
                if (searchType == SearchType.END) getString(R.string.to)
                else getString(R.string.from)
        }

        binding.entryRecyclerView.let {
            it.adapter = adapter
            it.itemAnimator = null
            it.layoutManager = LinearLayoutManager(
                requireActivity().applicationContext
            )
        }



        binding.searchInput.viewRequestInput()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is SearchUiEvent.SearchSuccess -> {
                            binding.searchLayout.error = null
                            binding.searchInput.viewHideInput()
                            navigator.popBackStack()
                        }
                        is SearchUiEvent.SearchInvalid -> {
                            binding.searchLayout.error = resources.getString(R.string.incorrect_number)
                            binding.searchInput.viewRequestInput()
                        }
                        is SearchUiEvent.FilterChanged -> {
                            vm.state.value.let { state ->
                                if (!state.filter.isNullOrBlank()) {
                                    adapter.submitList(
                                        state.entries.map {
                                            EntryItem(
                                                it,
                                                it.getEntryLocation(requireContext())
                                            )
                                        }
                                    ) { onListChangeComplete() }
                                }
                            }

                        }
                        is SearchUiEvent.TimeRecordsChanged -> {
                            vm.state.value.let { state ->
                                if (state.filter.isNullOrBlank()) {
                                    adapter.submitList(
                                        state.timeRecords.map {
                                            EntryItem(
                                                it.end,
                                                it.end.getEntryLocation(requireContext())
                                            )
                                        }
                                    ) { onListChangeComplete() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onListChangeComplete() {
        if (adapter.currentList.isNotEmpty()) {
            binding.textEmpty.isGone = true
        }
        else {
            binding.textEmpty.isVisible = true
        }
    }

    private fun processSearchResult(number: String) {
        vm.onEvent(SearchEvents.TrySearch(number, searchType))
    }

    companion object {
        private const val SEARCH_TYPE = "searchType"

        fun createBundleStart() =
            Bundle().apply { putInt(SEARCH_TYPE, SearchType.START.value) }
        fun createBundleEnd() =
            Bundle().apply { putInt(SEARCH_TYPE, SearchType.END.value) }
    }

}