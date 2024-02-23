package com.gerbort.common.model

data class Path(
    val start: TreeNode.Entry,
    val end: TreeNode.Entry,
    val points: List<OrientatedPosition>
)