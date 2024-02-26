package com.gerbort.router

import com.gerbort.common.model.TreeNode
import com.gerbort.pathfinding.domain.manager.PathState

data class RouterState(
    val selectedNode: TreeNode? = null,
    val linkPlacement: Boolean = false,
)
