package com.gerbort.router

import com.gerbort.common.model.TreeNode

data class RouterState(
    val selectedNode: TreeNode? = null,
    val linkPlacement: Boolean = false,
)
