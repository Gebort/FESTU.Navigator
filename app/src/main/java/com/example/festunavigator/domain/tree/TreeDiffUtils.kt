package com.example.festunavigator.domain.tree

import com.example.festunavigator.domain.utils.getApproxDif
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.radians
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TreeDiffUtils(
    private val tree: Tree
    ) {

    private var closestNodes = mutableMapOf<Int, TreeNode>()

    suspend fun getNearNodes(position: Float3, radius: Float): List<TreeNode> = withContext(Dispatchers.Default) {
        if (!tree.initialized) {
            return@withContext listOf()
        }
        else {
            val nodes = tree.getNodeFromEachRegion().toMutableMap()
            closestNodes.keys.forEach { key ->
                if (nodes.containsKey(key)) {
                    nodes[key] = closestNodes[key]!!
                }
            }
            closestNodes = nodes

            closestNodes.forEach { item ->
                closestNodes[item.key] = getNewCentralNode(position, item.value)
            }

            val nearNodes = mutableListOf<TreeNode>()
            closestNodes.values.forEach { node ->
                getNodesByRadius(position, node, radius, nearNodes)
            }

            return@withContext nearNodes
        }
    }

    private fun getNodesByRadius(position: Float3, central: TreeNode, radius: Float, list: MutableList<TreeNode>) {
            if (position.getApproxDif(central.position) > radius ) {
                return
            }
            list.add(central)
            central.neighbours.forEach { id ->
                tree.getNode(id)?.let { node ->
                    if (!list.contains(node)){
                        getNodesByRadius(position, node, radius, list)
                    }
                }
            }
    }

    private fun getNewCentralNode(position: Float3, central: TreeNode): TreeNode {
        var min = Pair(central, position.getApproxDif(central.position))
        central.neighbours.forEach { id ->
            tree.getNode(id)?.let { node ->
                searchClosestNode(position, node, min.second)?.let { res ->
                    if (res.second < min.second) {
                        min = res
                    }
                }
            }
        }
        return min.first
    }

    private fun searchClosestNode(position: Float3, node: TreeNode, lastDist: Float): Pair<TreeNode, Float>?  {
        val dist = position.getApproxDif(node.position)
        if (dist >= lastDist) {
            return null
        }
        else {
            var min: Pair<TreeNode, Float>? = null
            node.neighbours.forEach { id ->
                tree.getNode(id)?.let { node2 ->
                    searchClosestNode(position, node2, dist)?.let { res ->
                        if (min != null){
                            if (res.second < min!!.second){
                                min = res
                            }
                        }
                        else {
                            min = res
                        }
                    }
                }
            }
            min?.let {
                if (it.second < dist){
                    return min
                }
            }
            return Pair(node, dist)
        }
    }

//    private fun getClosestInArray(position: Float3, list: List<TreeNode>): TreeNode {
//        val first = list.first()
//        var min = Pair(first, position.getApproxDif(first.position))
//        list.forEach { node2 ->
//            val dist = position.getApproxDif(node2.position)
//            if (dist < min.second) {
//                min = Pair(node2, dist)
//            }
//        }
//        return min.first
//    }

}