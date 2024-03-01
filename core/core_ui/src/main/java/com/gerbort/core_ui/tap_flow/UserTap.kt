package com.gerbort.core_ui.tap_flow

import android.view.MotionEvent
import com.gerbort.common.model.TreeNode
import io.github.sceneview.node.Node
import io.github.sceneview.renderable.Renderable

data class UserTap(
    val node: Node?,
    val treeNode: TreeNode?,
    val renderable: Renderable?,
    val motionEvent: MotionEvent
)
