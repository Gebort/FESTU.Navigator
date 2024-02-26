package com.gerbort.pathfinding.domain.use_cases

import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.pathfinding.domain.Pathfinder

class PathfindUseCase(
    private val pathfinder: Pathfinder,
    private val nodeGraph: NodeGraph
) {

    suspend operator fun invoke(start: String, end: String) = pathfinder.findWay(start, end, nodeGraph)

}