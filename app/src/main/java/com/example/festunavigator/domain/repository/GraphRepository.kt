package com.example.festunavigator.domain.repository

import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode

interface GraphRepository {

    suspend fun getNodes(): Tree

    suspend fun insertNodes(nodes: List<TreeNode>)

    suspend fun deleteNodes(nodes: List<TreeNode>)

    suspend fun updateNodes(nodes: List<TreeNode>)

}