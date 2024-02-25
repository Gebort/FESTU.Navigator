package com.gerbort.router

import com.gerbort.common.model.TreeNode
import com.gerbort.hit_test.HitTestResult
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

sealed interface RouterEvent {

    data object ChangeLinkMode: RouterEvent
    class CreateNode(
        val number: String? = null,
        val position: Float3? = null,
        val orientation: Quaternion? = null,
        val hitTestResult: HitTestResult
    ): RouterEvent
    class DeleteNode(val node: TreeNode): RouterEvent
    class LinkNodes(val node1: TreeNode, val node2: TreeNode): RouterEvent

}