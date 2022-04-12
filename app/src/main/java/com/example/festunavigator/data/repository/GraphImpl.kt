package com.example.festunavigator.data.repository

import com.example.festunavigator.data.App
import com.example.festunavigator.data.model.TreeNodeDto
import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.Position
import kotlinx.coroutines.flow.*

class GraphImpl: GraphRepository {

    private val dao = App.instance?.getDatabase()?.graphDao!!

    override suspend fun getNodes(): Tree {
        return when (val result = dao.getNodes()) {
            null -> {
                Tree()
            }
            else -> {
                Tree(result)
            }
        }
    }

    override suspend fun insertNodes(nodes: List<TreeNode>) {
        dao.insertNodes(nodes.map { node -> TreeNodeDto.fromTreeNode(node) })
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) {
        dao.deleteNodes(nodes.map { node -> TreeNodeDto.fromTreeNode(node) })
    }

    override suspend fun updateNodes(nodes: List<TreeNode>) {
        dao.updateNodes(nodes.map { node -> TreeNodeDto.fromTreeNode(node) })

    }




}