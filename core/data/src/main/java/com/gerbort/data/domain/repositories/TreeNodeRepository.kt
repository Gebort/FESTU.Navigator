package com.gerbort.data.domain.repositories

import com.gerbort.common.model.TreeNode

interface TreeNodeRepository {

    suspend fun getNodes(): List<TreeNode>

    suspend fun insertNodes(
        nodes: List<TreeNode>,
    )

    suspend fun deleteNodes(nodes: List<TreeNode>)

    suspend fun updateNodes(
        nodes: List<TreeNode>,
    )

    suspend fun clearNodes()

}