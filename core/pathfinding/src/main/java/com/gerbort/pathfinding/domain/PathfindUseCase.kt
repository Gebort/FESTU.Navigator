package com.gerbort.pathfinding.domain

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.node_graph.domain.graph.NodeGraph
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PathfindUseCase @Inject constructor (
    private val nodeGraph: NodeGraph,
    private val pathfinder: Pathfinder,
) {

    suspend operator fun invoke(
        start: String,
        end: String,
    ): Path? =  pathfinder.findWay(
        from = start,
        to = end,
        nodeGraph = nodeGraph
    )

}