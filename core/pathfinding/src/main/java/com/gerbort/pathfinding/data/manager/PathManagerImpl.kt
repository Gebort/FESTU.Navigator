package com.gerbort.pathfinding.data.manager

import androidx.lifecycle.viewModelScope
import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.common.model.Record
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.pathfinding.domain.Pathfinder
import com.gerbort.pathfinding.domain.manager.PathManager
import com.gerbort.pathfinding.domain.manager.PathState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PathManagerImpl(
    private val nodeGraph: NodeGraph,
    private val pathfinder: Pathfinder,
    @Dispatcher(AppDispatchers.Default) private val dispatcher: CoroutineDispatcher
): PathManager {

    private val _state = MutableStateFlow(PathState())

    override fun getPathState(): Flow<PathState> = _state.asStateFlow()

    override fun setStart(label: String?) {
//        _pathState.update {
//            PathState(
//                startEntry = entry,
//                endEntry = if (entry.number == endEntry?.number) null else endEntry
//            )
//        }


        //Search successful
        pathfindJob?.cancel()
        pathfindJob = viewModelScope.launch {
            pathfind()
        }


        //saving route to the database
        pathState.startEntry?.let { start ->
            pathState.endEntry?.let { end ->
                val record = Record(
                    start = start.number,
                    end = end.number,
                    time = getCurrentWeekTime()
                )
                recordsRepository.insertRecord(record)
            }
        }
        TODO("Not yet implemented")
    }

    override fun setEnd(label: String?) {
        TODO("Not yet implemented")
    }

    override fun setStartAndEnd(startLabel: String?, endLabel: String?) {
        TODO("Not yet implemented")
    }
}