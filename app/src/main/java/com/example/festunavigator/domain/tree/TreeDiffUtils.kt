package com.example.festunavigator.domain.tree

import com.example.festunavigator.domain.utils.getApproxDif
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.radians

class TreeDiffUtils(
    private val tree: Tree
    ) {

    private var closestNodes = mutableMapOf<Int, TreeNode>()
 //   private var closestNode: TreeNode? = null

    suspend fun getNearNodes(position: Float3, radius: Float): List<TreeNode>{
        if (!tree.initialized) {
            return listOf()
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


//            if (closestNode == null) {
//                closestNode = getClosestInArray(position, tree.getAllEntries())
//            }
//            val closestNode = getNewCentralNode(position, closestNode!!)

            val nearNodes = mutableListOf<TreeNode>()
            closestNodes.values.forEach { node ->
                getNodesByRadius(position, node, radius, nearNodes)
            }

            //getClosestFreeNodes(position, radius, nearNodes)
            return nearNodes
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

    private fun getClosestFreeNodes(position: Float3, radius: Float, list: MutableList<TreeNode>) {
        tree.getFreeNodes().forEach { node ->
            if (position.getApproxDif(node.position) <= radius) {
                list.add(node)
            }
        }
    }

    private fun getClosestInArray(position: Float3, list: List<TreeNode>): TreeNode {
        val first = list.first()
        var min = Pair(first, position.getApproxDif(first.position))
        list.forEach { node2 ->
            val dist = position.getApproxDif(node2.position)
            if (dist < min.second) {
                min = Pair(node2, dist)
            }
        }
        return min.first
    }

}