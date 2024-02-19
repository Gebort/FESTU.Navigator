package com.gerbort.pathfinding.data

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.pathfinding.domain.Path
import com.gerbort.pathfinding.domain.Pathfinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class AStarImpl @Inject constructor(
    @Dispatcher(AppDispatchers.Default) private val dispatcher: CoroutineDispatcher
    //private val smoothPath: SmoothPath
): Pathfinder {



    override suspend fun findWay(from: String, to: String, graph: NodeGraph): Path? = withContext(dispatcher) {

        val startEntry = graph.getEntry(to) ?: return@withContext null
        val endEntry = graph.getEntry(from) ?: return@withContext null

        val finalNode = AStarNode(startEntry, null)
        val initialNode = AStarNode(endEntry, finalNode)

        val openList: MutableList<AStarNode> = mutableListOf()
        val closedSet: MutableSet<AStarNode> = mutableSetOf()

        openList.add(initialNode)

        while (openList.isNotEmpty()) {
            val currentNode = getNextAStarNode(openList)
            openList.remove(currentNode)
            closedSet.add(currentNode)

            if (currentNode == finalNode) {

                return@withContext Path(
                    start = startEntry,
                    end = endEntry,
                    nodes = getPath(currentNode).map { it.node }
//                    smoothPath(
//                        getPath(currentNode).map { aStarNode ->
//                            aStarNode.node
//                        }
//                    )
                )
            } else {
                addAdjacentNodes(currentNode, openList, closedSet, finalNode, graph)
            }
        }
        return@withContext null

    }

    private fun getPath(node: AStarNode): List<AStarNode> {
        var currentNode = node
        val path: MutableList<AStarNode> = mutableListOf()
        path.add(currentNode)
        while (currentNode.parent != null) {
            path.add(0, currentNode.parent!!)
            currentNode = currentNode.parent!!
        }
        return path
    }

    private fun addAdjacentNodes(
        currentNode: AStarNode,
        openList: MutableList<AStarNode>,
        closedSet: Set<AStarNode>,
        finalNode: AStarNode,
        graph: NodeGraph
    ) {
        currentNode.node.neighbours.forEach { nodeId ->
            graph.getNode(nodeId)?.let { node ->
                val nodeClosed = closedSet.find { it.node.id == nodeId }
                val nodeOpen = openList.find { it.node.id == nodeId }
                if ( nodeClosed == null && nodeOpen == null) {
                    checkNode(currentNode, AStarNode(node, finalNode), openList, closedSet)
                }
                else if (nodeOpen != null && nodeClosed == null) {
                    checkNode(currentNode, nodeOpen, openList, closedSet)
                }
            }

        }
    }

    //всем нодам устанавливается сложность в 1, можно ее убрать по идее
    private fun checkNode(
        parentNode: AStarNode,
        node: AStarNode,
        openList: MutableList<AStarNode>,
        closedSet: Set<AStarNode>
    ) {
        if (!closedSet.contains(node)) {
            if (!openList.contains(node)) {
                node.setNodeData(parentNode, 1f)
                openList.add(node)
            } else {
                node.checkBetterPath(parentNode, 1f)
            }
        }
    }


    private fun getNextAStarNode(openList: List<AStarNode>): AStarNode {
        return openList.sortedBy { node -> node.f }[0]

    }






}