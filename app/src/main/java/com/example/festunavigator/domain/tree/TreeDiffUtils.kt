package com.example.festunavigator.domain.tree

import com.example.festunavigator.domain.utils.getApproxDif
import dev.romainguy.kotlin.math.Float3

class TreeDiffUtils(
    private val tree: Tree
    ) {

    private var lastNode: TreeNode? = null

    suspend fun getNearNodes(position: Float3, radius: Int): List<TreeNode>{
        if (lastNode == null) {
            lastNode = getClosestInArray(position, tree.getAllEntries())
        }
        lastNode = getNewCentralNode(position, lastNode!!)
        val nearNodes = mutableListOf<TreeNode>()
        getNodesByRadius(lastNode!!, radius, nearNodes)
        return nearNodes
    }

    private fun getNodesByRadius(central: TreeNode, radius: Int, list: MutableList<TreeNode>) {
        if (radius == 0){
            return
        }
        else {
            list.add(central)
            central.neighbours.forEach { id ->
                tree.getNode(id)?.let { node ->
                    if (!list.contains(node)){
                        getNodesByRadius(node, radius-1, list)
                    }
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