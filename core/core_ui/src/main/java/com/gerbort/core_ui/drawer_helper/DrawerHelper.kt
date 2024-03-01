package com.gerbort.core_ui.drawer_helper

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.gerbort.common.model.OrientatedPosition
import com.gerbort.common.model.TreeNode
import com.google.ar.core.Anchor
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.NodeParent

interface DrawerHelper {

    fun setParentNode(
        parentNode: ArNode
    )

    fun setFragment(
        fragment: Fragment
    )

    suspend fun drawNode(
        treeNode: TreeNode,
        anchor: Anchor? = null
    ): ArNode

//    suspend fun drawSelection(
//        treeNode: TreeNode,
//    ): ArNode

    suspend fun placeLabel(
        label: String,
        pos: OrientatedPosition,
        anchor: Anchor? = null
    ): ArNode

    suspend fun placeArrow(
        pos: OrientatedPosition,
    ): ArNode

    fun removeNode(node: ArNode)

    fun removeArrowWithAnim(node: ArNode)

    fun removeLabelWithAnim(node: ArNode)

    suspend fun drawLine(
        from: Position,
        to: Position,
    ): ArNode

    suspend fun placeBlankNode(
        position: Float3? = null,
        anchor: Anchor? = null
    ): ArNode

    suspend fun joinAnimation(node: ArNode)

    suspend fun updateSelectionMarker(node: TreeNode?)

}