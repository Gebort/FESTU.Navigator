package com.example.festunavigator.presentation

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import io.github.sceneview.ar.node.ArNode

data class ConfirmationObject(
    val label: String,
    val pos: OrientatedPosition,
    val node: ArNode
)
