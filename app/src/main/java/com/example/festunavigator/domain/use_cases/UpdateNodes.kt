package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3

class UpdateNodes(
    private val repository: GraphRepository
) {
    suspend operator fun invoke(nodes: List<TreeNode>, translocation: Float3) {
        val transNodes = nodes.toMutableList()
        repository.updateNodes(
            transNodes.map { node ->
                when (node) {
                    is TreeNode.Entry -> {
                        node.copy(
                            position = translocatePosition(
                                node.position, translocation
                            )
                        )
                    }
                    is TreeNode.Path -> {
                        node.copy(
                            position = translocatePosition(
                                node.position, translocation
                            )
                        )
                    }
                }
            }
        )
    }

    private fun translocatePosition(pos: Float3, translocation: Float3): Float3 {
        return Float3(
            pos.x + translocation.x,
            pos.y + translocation.y,
            pos.z + translocation.z,
        )
    }
}