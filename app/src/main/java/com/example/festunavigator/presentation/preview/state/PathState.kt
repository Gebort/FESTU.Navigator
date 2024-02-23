package com.example.festunavigator.presentation.preview.state

import com.gerbort.common.model.Path
import com.gerbort.common.model.TreeNode
import com.gerbort.path_correction.data.PathDiffUtils

data class PathState(
    val startEntry: TreeNode.Entry? = null,
    val endEntry: TreeNode.Entry? = null,
    val path: Path? = null
) {

    val pathDiffUtils = path?.let { PathDiffUtils(it) }

}
