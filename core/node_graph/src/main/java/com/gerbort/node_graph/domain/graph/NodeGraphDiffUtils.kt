package com.gerbort.node_graph.domain.graph

import com.gerbort.common.model.TreeNode
import dev.romainguy.kotlin.math.Float3

interface NodeGraphDiffUtils {

    suspend fun getNearNodes(position: Float3, radius: Float): List<TreeNode>

}