package com.gerbort.node_graph.domain.use_cases

import com.gerbort.common.model.TreeNode
import com.gerbort.node_graph.domain.graph.NodeGraph

class DeleteNodeUseCase(
    private val nodeGraph: NodeGraph
) {

    suspend operator fun invoke(treeNode: TreeNode) = nodeGraph.removeNode(treeNode)

}