package com.example.festunavigator.presentation.preview

import com.example.festunavigator.presentation.LabelObject
import com.gerbort.common.model.TreeNode
import com.gerbort.hit_test.HitTestResult
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame

sealed interface MainEvent{
    class NewFrame(val frame: ArFrame): MainEvent

    class TrySearch(val number: String, val changeType: Int): MainEvent

    class NewAzimuth(val azimuthRadians: Float): MainEvent
    class NewSelectedNode(val node: TreeNode?): MainEvent
    object ChangeLinkMode: MainEvent
    class CreateNode(
        val number: String? = null,
        val position: Float3? = null,
        val orientation: Quaternion? = null,
        val hitTestResult: HitTestResult
    ): MainEvent
    class DeleteNode(val node: TreeNode): MainEvent
    class LinkNodes(val node1: TreeNode, val node2: TreeNode): MainEvent
    class PivotTransform(val transition: Quaternion): MainEvent
    object LoadRecords: MainEvent
}
