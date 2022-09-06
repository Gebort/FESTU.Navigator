package com.example.festunavigator.data.repository

import com.example.festunavigator.data.data_source.Database
import com.example.festunavigator.data.model.TreeNodeDto
import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.use_cases.convert
import com.example.festunavigator.domain.use_cases.opposite
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3
import javax.inject.Inject

class GraphImpl @Inject constructor(
    private val database: Database
): GraphRepository {

    private val dao = database.graphDao

    override suspend fun getNodes(): List<TreeNodeDto> {
        return dao.getNodes() ?: listOf()
    }

    override suspend fun insertNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ){
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuaternion = rotation.opposite()
        dao.insertNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        TreeNodeDto.fromTreeNode(
                            node = node,
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuaternion, pivotPosition
                            ),
                            forwardVector = node.forwardVector.convert(undoQuaternion)
                        )
                    }
                    is TreeNode.Path -> {
                        TreeNodeDto.fromTreeNode(
                            node = node,
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuaternion, pivotPosition
                            )
                        )

                    }
                }
            }
        )
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) {
        dao.deleteNodes(nodes.map { node -> TreeNodeDto.fromTreeNode(node) })
    }

    override suspend fun updateNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuarterion = rotation.opposite()
        dao.updateNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        TreeNodeDto.fromTreeNode(
                            node = node,
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuarterion, pivotPosition
                            ),
                            forwardVector = node.forwardVector.convert(undoQuarterion)
                        )
                    }
                    is TreeNode.Path -> {
                        TreeNodeDto.fromTreeNode(
                            node = node,
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuarterion, pivotPosition
                            )
                        )
                    }
                }
            }
        )
    }

    override suspend fun clearNodes() {
        dao.clearNodes()
    }

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