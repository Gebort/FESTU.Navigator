package com.gerbort.node_graph.domain.use_cases

import com.gerbort.common.model.TreeNode
import com.gerbort.node_graph.domain.graph.NodeGraph

class LinkNodesUseCase(
    private val nodeGraph: NodeGraph
) {

    suspend operator fun invoke(
        treeNode1: TreeNode,
        treeNode2: TreeNode,
    ): Boolean = nodeGraph.addLink(treeNode1, treeNode2)

}