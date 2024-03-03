package com.gerbort.node_graph.domain.graph

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toQuaternion

data class NodeGraphPosition(
    val translocation: Float3 = Float3(0f, 0f, 0f),
    val pivotPosition: Float3 = Float3(0f, 0f, 0f),
    val rotation: Quaternion = Float3(0f, 0f, 0f).toQuaternion(),
)
