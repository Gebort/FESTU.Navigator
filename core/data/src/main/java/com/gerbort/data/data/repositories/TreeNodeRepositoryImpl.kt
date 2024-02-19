package com.gerbort.data.data.repositories

import com.gerbort.common.model.TreeNode
import com.gerbort.data.data.mappers.toCommon
import com.gerbort.data.data.mappers.toEntity
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.database.dao.TreeNodeDao
import javax.inject.Inject

internal class TreeNodeRepositoryImpl @Inject constructor(
    private val treeNodeDao: TreeNodeDao
): TreeNodeRepository {
    override suspend fun getNodes(): List<TreeNode> {
        return treeNodeDao.getNodes().map { it.toCommon() }
    }

    override suspend fun insertNodes(nodes: List<TreeNode>) {
        return treeNodeDao.insertNodes(nodes.map { it.toEntity() })
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) {
        return treeNodeDao.deleteNodesById(nodes.map { it.id })
    }

    override suspend fun updateNodes(nodes: List<TreeNode>) {
        return treeNodeDao.updateNodes(nodes.map { it.toEntity() })
    }

    override suspend fun clearNodes() {
        return treeNodeDao.clearNodes()
    }


}