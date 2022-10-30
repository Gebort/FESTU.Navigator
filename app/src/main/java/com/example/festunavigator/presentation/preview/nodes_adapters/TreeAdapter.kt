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
import io.github.sceneview.math.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TreeAdapter(
    drawerHelper: DrawerHelper,
    previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
    private val onlyEntries: Boolean,
): NodesAdapter<TreeNode>(drawerHelper, previewView, bufferSize, scope) {

    private val modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode> = mutableBiMapOf()
    private var selectionNode: ArNode? = null

    override suspend fun onInserted(item: TreeNode): ArNode {
        if (onlyEntries && item is TreeNode.Entry) {
            return drawerHelper.drawNode(item, previewView)
        }
        val node1 = drawerHelper.drawNode(item, previewView)
        for (id in item.neighbours) {
            nodes.keys.firstOrNull { it.id == id }?.let { treeNode ->
                nodes[treeNode]?.let { node2 ->
                    if (modelsToLinkModels[Pair(node1, node2)] == null ){
                        drawerHelper.drawLine(
                            node1.position - (parentNode?.position ?: Position(0f)),
                            node2.position,
                            parentNode ?: previewView
                        ).let { node ->
                            modelsToLinkModels[Pair(node1, node2)] = node
                        }
                    }
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

    suspend fun createLink(treeNode1: TreeNode, treeNode2: TreeNode) {
        val node1 = nodes[treeNode1]
        val node2 = nodes[treeNode2]
        if (node1 != null && node2 != null) {
            drawerHelper.drawLine(
                node1.position,
                node2.position,
                parentNode ?: previewView
            ).let { node ->
                modelsToLinkModels[Pair(node1, node2)] = node
            }
        }
    }

    override fun changeParentPos(newParentPos: Float3?, orientation: Quaternion?) {
        if (parentNode == null) {
            throw Exception("Parent node is not set")
        }
        newParentPos?.let {
            val diff = it - parentNode!!.position
            nodes.values.forEach { arNode ->
                arNode.position -= diff
            }
            modelsToLinkModels.values.forEach { arNode ->
                arNode.position -= diff
            }
            parentNode?.position = it
        }
        orientation?.let { q ->
            parentNode?.quaternion = q
        }
    }

    fun getArNode(treeNode: TreeNode?): ArNode? = nodes[treeNode]

    fun getTreeNode(node: ArNode?): TreeNode? {
        node?.let {
            return nodes.entries.find { it.value == node }?.key
        }
        return null
    }

    suspend fun updateSelection(node: TreeNode?) {
            selectionNode?.let {
                parentNode?.removeChild(it)
                drawerHelper.removeNode(it)
            }
            node?.let {
                val trans = parentNode?.position ?: Float3(0f)
                val n = it.copy(position = it.position - trans)
                selectionNode = drawerHelper.drawSelection(n, previewView).also { arNode ->
                    parentNode?.addChild(arNode)
                }
        }
    }
}