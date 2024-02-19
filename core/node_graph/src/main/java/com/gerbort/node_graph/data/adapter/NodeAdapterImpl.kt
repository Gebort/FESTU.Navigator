package com.gerbort.node_graph.data.adapter

import com.gerbort.common.model.TreeNode
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.common.utils.multiply
import com.gerbort.common.utils.opposite
import com.gerbort.node_graph.domain.adapter.NodeRepositoryAdapter
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3
import javax.inject.Inject

internal class NodeAdapterImpl @Inject constructor(
    private val treeNodeRepository: TreeNodeRepository
): NodeRepositoryAdapter {
    override suspend fun getNodes(): List<TreeNode> = treeNodeRepository.getNodes()
    override suspend fun insertNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuaternion = rotation.opposite()
        treeNodeRepository.insertNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        node.copy(
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuaternion, pivotPosition
                            ),
                            forwardVector = node.forwardVector.multiply(undoQuaternion),
                            northDirection = node.northDirection?.multiply(undoQuaternion),
                        )
                    }
                    is TreeNode.Path -> {
                        node.copy(
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuaternion, pivotPosition
                            ),
                            northDirection = node.northDirection?.multiply(undoQuaternion)
                        )
                    }
                }
            }
        )
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) = treeNodeRepository.deleteNodes(nodes)

    override suspend fun updateNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuarterion = rotation.opposite()
        treeNodeRepository.updateNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        node.copy(
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuarterion, pivotPosition
                            ),
                            forwardVector = node.forwardVector.multiply(undoQuarterion)
                        )
                    }
                    is TreeNode.Path -> {
                        node.copy(
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuarterion, pivotPosition
                            )
                        )
                    }
                }
            }
        )
    }

    override suspend fun clearNodes() = treeNodeRepository.clearNodes()

    //TODO заменить на использование Quarterion.undoConvertPosition
    private fun undoPositionConvert(
        position: Float3,
        translocation: Float3,
        quaternion: Quaternion,
        pivotPosition: Float3
    ): Float3 {
        return (com.google.ar.sceneform.math.Quaternion.rotateVector(
            quaternion.toOldQuaternion(),
            (position - pivotPosition - translocation).toVector3()
        ).toFloat3() + pivotPosition)
    }


}