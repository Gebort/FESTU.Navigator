package com.gerbort.node_graph.domain.use_cases

import com.gerbort.common.model.TreeNode
import com.gerbort.common.utils.fromVector
import com.gerbort.node_graph.domain.graph.NodeGraph
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toVector3

class CreateNodeUseCase(
    private val nodeGraph: NodeGraph
) {

    suspend operator fun invoke(
        position: Float3,
        number: String? = null,
        orientation: Quaternion? = null,
        northDirection: Quaternion? = null,
    ): Result<TreeNode> = nodeGraph.addNode(
        position = position,
        northDirection = northDirection,
        number = number,
        forwardDirection = orientation
    )


}