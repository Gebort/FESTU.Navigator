package com.gerbort.pathfinding.domain

import com.gerbort.common.model.Path
import com.gerbort.node_graph.domain.graph.NodeGraph

interface Pathfinder {

    suspend fun findWay(
        from: String,
        to: String,
        nodeGraph: NodeGraph
    ): Path?

}