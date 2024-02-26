package com.gerbort.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gerbort.common.model.Record
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.data.domain.repositories.getCurrentWeekTime
import com.gerbort.node_graph.domain.use_cases.GetEntriesUseCase
import com.gerbort.node_graph.domain.use_cases.GetEntryUseCase
import com.gerbort.pathfinding.domain.manager.PathManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel(
    private val recordsRepository: RecordsRepository,
    private val pathManager: PathManager,
    private val getEntryUseCase: GetEntryUseCase,
    private val getEntriesUseCase: GetEntriesUseCase
): ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val _uiEvents = Channel<SearchUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var entries = setOf<String>()

    private var recordsJob: Job? = null
    private var entriesJob: Job? = null

    fun onEvent(event: SearchEvents) {
        viewModelScope.launch {
            when (event) {
                is SearchEvents.LoadRecordsAndEntries -> loadRecordsAndEntries()
                is SearchEvents.TrySearch -> processSearch(event.number, event.searchType)
                is SearchEvents.Filter -> filter(event.filter)
            }
        }
    }

    private suspend fun processSearch(number: String, searchType: SearchType) {
        val entry = getEntryUseCase(number)
        if (entry == null) {
            _uiEvents.send(SearchUiEvent.SearchInvalid)
            return
        }
        if (searchType == SearchType.START) {
            pathManager.setStart(entry.number)
        } else {
            pathManager.setEnd(entry.number)
        }
        _uiEvents.send(SearchUiEvent.SearchSuccess)
    }

    private suspend fun filter(filter: String?) {
        _state.update { it.copy(
            filter = filter,
            entries = entries.filter { it.startsWith(filter.orEmpty()) }
        ) }
        _uiEvents.send(SearchUiEvent.FilterChanged)
    }


    private suspend fun loadRecordsAndEntries() {
        recordsJob?.cancel()
        entriesJob?.cancel()
        filter(null)
        recordsJob = viewModelScope.launch {
            val time = recordsRepository.getCurrentWeekTime() + 30*60*1000L
            recordsRepository.getRecords(time, 5).collectLatest{ records ->
                _state.update { it.copy(timeRecords = records) }
                _uiEvents.send(SearchUiEvent.TimeRecordsChanged)
            }
        }
        entriesJob = viewModelScope.launch {
            //TODO заменить на flow
            entries = getEntriesUseCase()
            filter(state.value.filter)
        }
    }
}