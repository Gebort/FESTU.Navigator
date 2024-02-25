package com.gerbort.pathfinding.domain.manager

import com.gerbort.common.model.Path
import com.gerbort.common.model.TreeNode
import com.gerbort.pathfinding.domain.diff_utils.PathDiffUtils

data class PathState(
    val startEntry: TreeNode.Entry? = null,
    val endEntry: TreeNode.Entry? = null,
    val path: Path? = null
) {

    val pathDiffUtils = path?.let { PathDiffUtils(it) }

}
