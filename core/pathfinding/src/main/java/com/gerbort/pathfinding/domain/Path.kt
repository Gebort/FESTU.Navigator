package com.gerbort.pathfinding.domain

import com.gerbort.common.model.TreeNode

data class Path(
    val start: TreeNode.Entry,
    val end: TreeNode.Entry,
    val nodes: List<TreeNode>
    //val nodes: List<OrientatedPosition>
)