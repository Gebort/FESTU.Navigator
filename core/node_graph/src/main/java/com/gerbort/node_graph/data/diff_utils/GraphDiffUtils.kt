package com.gerbort.node_graph.data.diff_utils

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.data.domain.model.TreeNode
import com.gerbort.common.utils.getApproxDif
import com.gerbort.node_graph.domain.graph.NodeGraph
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.sqr
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class GraphDiffUtils @Inject constructor(
    private val graph: NodeGraph,
    @Dispatcher(AppDispatchers.Default) private val dispatcher: CoroutineDispatcher
) {

    private var closestNodes = mutableMapOf<Int, TreeNode>()

    suspend fun getNearNodes(position: Float3, radius: Float): List<TreeNode> = withContext(dispatcher) {
        if (!graph.isInitialized()) {
            return@withContext listOf()
        }
        else {
            val nodes = graph.getNodeFromEachRegion().toMutableMap()
            closestNodes.keys.forEach { key ->
                closestNodes[key]?.let {
                    if (nodes.containsKey(key) && graph.hasNode(it)) {
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
            graph.getNode(it)?.let { m ->
                var minDist = segmentDistance(min1.position, m.position, pos)
                var min2 = m
                min1.neighbours.forEach { id ->
                    graph.getNode(id)?.let { n ->
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
            graph.getNode(id)?.let { node ->
                if (!list.contains(node)){
                    getNodesByRadius(position, node, radius, list)
                }
            }
        }
    }

    private fun getNewCentralNode(position: Float3, central: TreeNode): TreeNode {
        var min = Pair(central, position.getApproxDif(central.position))
        central.neighbours.forEach { id ->
            graph.getNode(id)?.let { node ->
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
                graph.getNode(id)?.let { node2 ->
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