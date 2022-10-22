package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.data.utils.multiply
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.google.android.material.snackbar.Snackbar
import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode

class TreeAdapter(
    drawerHelper: DrawerHelper,
    previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
    needParentNode: Boolean
): NodesAdapter<TreeNode>(drawerHelper, previewView, bufferSize, scope, needParentNode) {

    private val modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode> = mutableBiMapOf()

    override suspend fun onInserted(item: TreeNode): ArNode {
        val node1 = drawerHelper.drawNode(item, previewView)
        for (id in item.neighbours) {
            nodes.keys.firstOrNull { it.id == id }?.let { treeNode ->
                nodes[treeNode]?.let { node2 ->
                    if (modelsToLinkModels[Pair(node1, node2)] == null ){
                        drawerHelper.drawLine(
                            node1,
                            node2,
                            modelsToLinkModels,
                            previewView
                        )
                        modelsToLinkModels[Pair(node1, node2)]?.let { parentNode?.addChild(it) }
                    }
                }
            }
        }
        return node1
    }

    override suspend fun onRemoved(item: TreeNode, node: ArNode) {
        drawerHelper.removeNode(node)
        modelsToLinkModels.keys
            .filter { it.first == node || it.second == node }
            .forEach { pair ->
                modelsToLinkModels[pair]?.let { parentNode?.removeChild(it) }
                drawerHelper.removeLink(pair, modelsToLinkModels)
            }
    }

    suspend fun createLink(treeNode1: TreeNode, treeNode2: TreeNode) {
        val node1 = nodes[treeNode1]
        val node2 = nodes[treeNode2]
        if (node1 != null && node2 != null) {
            drawerHelper.drawLine(
                node1,
                node2,
                modelsToLinkModels,
                previewView
            )
            modelsToLinkModels[Pair(node1, node2)]?.let { parentNode?.addChild(it) }
        }
    }

    override fun changeParentPos(newParentPos: Float3?, transition: Quaternion?) {
        if (parentNode == null) {
            throw Exception("Parent node is not set")
        }
        newParentPos?.let {
            val diff = it - parentNode!!.position
            nodes.values.forEach { node ->
                node.position -= diff
            }
            modelsToLinkModels.values.forEach { node ->
                node.position -= diff
            }
            parentNode?.position = it
        }
        transition?.let { q2 ->
            parentNode?.quaternion?.let { q1 ->
                parentNode?.quaternion = q1.multiply(q2)
            }
        }
    }

    fun getArNode(treeNode: TreeNode?): ArNode? = nodes[treeNode]

    fun getTreeNode(node: ArNode?): TreeNode? {
        node?.let {
            return nodes.entries.find { it.value == node }?.key
        }
        return null
    }
}