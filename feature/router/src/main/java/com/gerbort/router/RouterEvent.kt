package com.gerbort.router

import com.gerbort.common.model.TreeNode
import dev.romainguy.kotlin.math.Float3

sealed interface RouterEvent {

    data object ChangeLinkMode: RouterEvent
    class CreatePathNode(val position: Float3, ): RouterEvent
    class DeleteNode(val node: TreeNode): RouterEvent
    class LinkNodes(val node1: TreeNode, val node2: TreeNode): RouterEvent

}