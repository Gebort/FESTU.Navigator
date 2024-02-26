package com.gerbort.search

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.data.domain.repositories.getCurrentWeekTime
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
    private val getEntryUseCase: GetEntryUseCase
): ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val _uiEvents = Channel<SearchUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var recordsJob: Job? = null

    fun onEvent(event: SearchEvents) {
        viewModelScope.launch {
            when (event) {
                is SearchEvents.LoadRecords -> loadRecords()
                is SearchEvents.TrySearch -> processSearch(event.number, event.searchType)
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

    private suspend fun loadRecords() {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            val time = recordsRepository.getCurrentWeekTime() + 30*60*1000L
            recordsRepository.getRecords(time, 5).collectLatest{ records ->
                _state.update { it.copy(timeRecords = records) }
            }
        }
    }
}