package com.gerbort.node_graph.domain.adapter

import com.gerbort.common.model.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

interface NodeRepositoryAdapter {
    suspend fun getNodes(): List<TreeNode>

    suspend fun insertNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    )

    suspend fun deleteNodes(nodes: List<TreeNode>)

    suspend fun updateNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    )

    suspend fun clearNodes()
}