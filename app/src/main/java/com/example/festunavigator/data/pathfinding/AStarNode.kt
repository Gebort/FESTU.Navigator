package com.example.festunavigator.data.pathfinding

import com.example.festunavigator.domain.tree.TreeNode
import kotlin.math.abs

class AStarNode(
    val node: TreeNode,
    finalNode: AStarNode?
){
    var g = 0f
        private set
    var f = 0f
        private set
    var h = 0f
        private set
    var parent: AStarNode? = null
        private set

    init {
        finalNode?.let {
            calculateHeuristic(finalNode)
        }
    }

    fun calculateHeuristic(finalNode: AStarNode) {
        h =     abs(finalNode.node.position.x - node.position.x) +
                abs(finalNode.node.position.y - node.position.y) +
                abs(finalNode.node.position.z - node.position.z)
    }

    fun setNodeData(currentNode: AStarNode, cost: Float) {
        g = currentNode.g + cost
        parent = currentNode
        calculateFinalCost()
    }

    fun checkBetterPath(currentNode: AStarNode, cost: Float): Boolean {
        val gCost = currentNode.g + cost
        if (gCost < g) {
            setNodeData(currentNode, cost)
            return true
        }
        return false
    }

    private fun calculateFinalCost() {
        f = g + h
    }

    override fun equals(other: Any?): Boolean {
        val otherNode: AStarNode? = other as AStarNode?
        otherNode?.let {
            return this.node == otherNode.node
        }
        return false
    }
}