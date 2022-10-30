package com.example.festunavigator.domain.tree

import com.example.festunavigator.domain.utils.getApproxDif
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.sqr
import io.github.sceneview.light.position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

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
                closestNodes[key]?.let {
                    if (nodes.containsKey(key) && tree.hasNode(it)) {
                        nodes[key] = it
                    }
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

    fun getClosestSegment(pos: Float3): Pair<TreeNode, TreeNode>? {
        val min1 = getClosestInArray(pos, closestNodes.values) ?: return null
        min1.neighbours.firstOrNull()?.let { it ->
            tree.getNode(it)?.let { m ->
                var minDist = segmentDistance(min1.position, m.position, pos)
                var min2 = m
                min1.neighbours.forEach { id ->
                    tree.getNode(id)?.let { n ->
                        val dist = segmentDistance(min1.position, n.position, pos)
                        if (dist < minDist) {
                            minDist = dist
                            min2 = n
                        }
                    }
                }
                return Pair(min1, min2)
            }
        }
        return null
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

    private fun getClosestInArray(position: Float3, list: Collection<TreeNode>): TreeNode? {
        val first = list.firstOrNull()
        first?.let {
            var min = first
            var minDist = position.getApproxDif(first.position)
            list.forEach { node2 ->
                val dist = position.getApproxDif(node2.position)
                if (dist < minDist) {
                    min = node2
                    minDist = dist
                }
            }
            return min
        }
        return null
    }

    private fun segmentDistance(start: Float3, end: Float3, point: Float3): Float {
        return abs((end.x - start.x)*(start.z - point.z) - (start.x - point.x)*(end.z - start.z)) /
                sqrt(sqr(end.x - start.x) + sqr(end.z - start.z))
    }

}