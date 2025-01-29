package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.gerbort.common.model.TreeNode
import com.gerbort.core_ui.drawer_helper.DrawerHelper
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import kotlinx.coroutines.launch

class TreeAdapter(
    drawerHelper: DrawerHelper,
    previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
    private val onlyEntries: Boolean,
): NodesAdapter<TreeNode>(drawerHelper, previewView, bufferSize, scope) {

    private val modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode> = mutableBiMapOf()

    override suspend fun onInserted(item: TreeNode): ArNode {
        if (onlyEntries && item is TreeNode.Entry) {
            return drawerHelper.drawNode(item)
        }
        val node1 = drawerHelper.drawNode(item)
        for (id in item.neighbours) {
            nodes.keys.firstOrNull { it.id == id }?.let { treeNode ->
                nodes[treeNode]?.let { node2 ->
                    //position of node1 will be changed in NodesAdapter, so we need to adjust it accordingly
                    drawNewLink(node1, node2, changeNode1Pos = true)
                }
            }
        }
        return node1
    }

    override suspend fun onRemoved(item: TreeNode, node: ArNode) {
        modelsToLinkModels.keys
            .filter { it.first == node || it.second == node }
            .forEach { pair ->
                modelsToLinkModels[pair]?.let {
                    parentNode?.removeChild(it)
                    drawerHelper.removeNode(it)
                }
                modelsToLinkModels.remove(pair)
            }
        drawerHelper.removeNode(node)
    }

    override fun onParentPosChange(posDifference: Float3) {
        modelsToLinkModels.values.forEach { arNode ->
            arNode.position -= posDifference
        }
    }

    fun newLinkAdded(treeNode1: TreeNode, treeNode2: TreeNode) {
        scope.launch {
            val arNode1 = nodes[treeNode1] ?: return@launch
            val arNode2 = nodes[treeNode2] ?: return@launch
            drawNewLink(arNode1, arNode2)
        }

    }

    fun getTreeNode(node: ArNode?): TreeNode? {
        node?.let {
            return nodes.entries.find { it.value == node }?.key
        }
        return null
    }

    private suspend fun drawNewLink(
        node1: ArNode,
        node2: ArNode,
        changeNode1Pos: Boolean = false
    ) {
        if (modelsToLinkModels[Pair(node1, node2)] == null ){
            drawerHelper.drawLine(
                if (changeNode1Pos) node1.position - (parentNode?.position ?: Position(0f)) else node1.position,
                node2.position,
            ).let { linkArNode ->
                parentNode?.addChild(linkArNode)
                modelsToLinkModels[Pair(node1, node2)] = linkArNode
            }
        }
    }
}