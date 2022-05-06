package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

class UpdateNodes(
    private val repository: GraphRepository,
    private val convertQuaternion: ConvertQuaternion = ConvertQuaternion(),
    private val undoPositionConvert: UndoPositionConvert = UndoPositionConvert()
) {
    suspend operator fun invoke(nodes: List<TreeNode>, translocation: Float3, rotation: Quaternion, pivotPosition: Float3) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuarterion = rotation.opposite()
        repository.updateNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        node.copy(
                            position = undoPositionConvert(
                                node.position, undoTranslocation, undoQuarterion, pivotPosition
                            ),
                            forwardVector = convertQuaternion(
                                node.forwardVector, undoQuarterion
                            )
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

    companion object {
        private val TAG = "REPOSITORY"
    }
}