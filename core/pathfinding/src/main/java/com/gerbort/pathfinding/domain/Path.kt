package com.gerbort.pathfinding.domain

import com.gerbort.common.model.OrientatedPosition
import com.gerbort.common.model.TreeNode

data class Path(
    val start: TreeNode.Entry,
    val end: TreeNode.Entry,
    val points: List<OrientatedPosition>
)