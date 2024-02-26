package com.gerbort.pathfinding.data.manager

import androidx.lifecycle.viewModelScope
import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.ApplicationScope
import com.gerbort.common.di.Dispatcher
import com.gerbort.common.model.Record
import com.gerbort.common.model.TreeNode
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.data.domain.repositories.getCurrentWeekTime
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.use_cases.GetEntryUseCase
import com.gerbort.pathfinding.domain.Pathfinder
import com.gerbort.pathfinding.domain.manager.PathManager
import com.gerbort.pathfinding.domain.manager.PathState
import com.gerbort.pathfinding.domain.use_cases.PathfindUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PathManagerImpl(
    private val pathfindUseCase: PathfindUseCase,
    private val getEntryUseCase: GetEntryUseCase,
    private val recordsRepository: RecordsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
): PathManager {

    private val _state = MutableStateFlow(PathState())

    private var pathfindJob: Job? = null

    override fun getPathState(): Flow<PathState> = _state.asStateFlow()

    override fun setStart(label: String?) {
        changePathState(
            startLabel = label,
            endLabel = null,
            changeStart = true,
            changeEnd = false,
        )
    }

    override fun setEnd(label: String?) {
        changePathState(
            startLabel = null,
            endLabel = label,
            changeStart = false,
            changeEnd = true,
        )
    }

    override fun setStartAndEnd(startLabel: String?, endLabel: String?) {
        changePathState(
            startLabel = startLabel,
            endLabel = endLabel,
            changeStart = true,
            changeEnd = true,
        )
    }

    private fun changePathState(
        startLabel: String?,
        endLabel: String?,
        changeStart: Boolean,
        changeEnd: Boolean
    ) {
        pathfindJob?.cancel()
        pathfindJob = applicationScope.launch {
            if (_state.value.startEntry?.number == startLabel &&
                _state.value.endEntry?.number == endLabel) return@launch
            val start = when {
                !changeStart && endLabel != null && _state.value.startEntry?.number == endLabel -> null
                !changeStart -> _state.value.startEntry
                startLabel != null -> getEntryUseCase(startLabel)
                else -> null
            }
            val end = when {
                startLabel == endLabel -> null
                !changeEnd && startLabel != null && _state.value.endEntry?.number == startLabel -> null
                !changeEnd -> _state.value.endEntry
                endLabel != null -> getEntryUseCase(endLabel)
                else -> null
            }
            _state.update {
                PathState(
                    startEntry = start,
                    endEntry = end,
                    isLoading = start != null && end != null
                )
            }
            if (start != null && end != null) findWay(start, end)
        }
    }

    private suspend fun findWay(start: TreeNode.Entry, end: TreeNode.Entry) {
        val path = pathfindUseCase(start.number, end.number)
        if (path != null) {
            _state.update { it.copy(
                path = path,
                pathNotFound = false,
                isLoading = false,
            ) }
            val record = Record(
                start = start.number,
                end = end.number,
                time = recordsRepository.getCurrentWeekTime()
            )
            recordsRepository.insertRecord(record)
        }
        else {
            _state.update { it.copy(
                path = null,
                pathNotFound = true,
                isLoading = false,
            ) }
        }
    }
}