package com.example.festunavigator.presentation.preview.state

import com.gerbort.common.model.Path
import com.gerbort.common.model.TreeNode

data class PathState(
    val startEntry: TreeNode.Entry? = null,
    val endEntry: TreeNode.Entry? = null,
    val path: Path? = null
)
