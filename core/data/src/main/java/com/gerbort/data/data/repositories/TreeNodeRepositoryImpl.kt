package com.gerbort.data.data.repositories

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.common.model.TreeNode
import com.gerbort.data.data.mappers.toCommon
import com.gerbort.data.data.mappers.toEntity
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.database.dao.TreeNodeDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class TreeNodeRepositoryImpl @Inject constructor(
    private val treeNodeDao: TreeNodeDao,
    @Dispatcher(AppDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    ): TreeNodeRepository {
    override suspend fun getNodes(): List<TreeNode> = withContext(dispatcher) {
        return@withContext treeNodeDao.getNodes().map { it.toCommon() }
    }

    override suspend fun insertNodes(nodes: List<TreeNode>) = withContext(dispatcher) {
        return@withContext treeNodeDao.insertNodes(nodes.map { it.toEntity() })
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) = withContext(dispatcher) {
        return@withContext treeNodeDao.deleteNodesById(nodes.map { it.id })
    }

    override suspend fun updateNodes(nodes: List<TreeNode>) = withContext(dispatcher) {
        return@withContext treeNodeDao.updateNodes(nodes.map { it.toEntity() })
    }

    override suspend fun clearNodes() = withContext(dispatcher) {
         return@withContext treeNodeDao.clearNodes()
    }


}