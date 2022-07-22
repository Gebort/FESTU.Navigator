package com.example.festunavigator.presentation.preview.state

import com.example.festunavigator.domain.pathfinding.Path
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.LabelObject

data class PathState(
    val startLabel: LabelObject? = null,
    val endLabel: LabelObject? = null,
    val path: Path? = null
)
