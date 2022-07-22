package com.example.festunavigator.domain.pathfinding

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.light.position
import kotlin.math.abs
import kotlin.math.sqrt

class Path(val nodes: List<OrientatedPosition>) {

    private var lastNodeId: Int? = null

    fun getNearNodes(number: Int, position: Float3): List<OrientatedPosition> {
            val central = linearNodeSearch(position, lastNodeId ?: 0)
            lastNodeId = central
            return grabNearNodes(number, central)
        }

    private fun grabNearNodes(number: Int, centralId: Int): List<OrientatedPosition> {
        val sides = number-1
        var left = centralId - sides/2
        var right = centralId + sides/2 + if (sides%2==0) 0 else 1

        if (left < 0)
            left = 0
        if (right > nodes.size - 1) {
            right = nodes.size - 1
        }

        return List(right - left + 1) {
            nodes[it + left]
        }
    }

    private fun linearNodeSearch(pos: Float3, start: Int = 0): Int {
        if (nodes.isEmpty()){
            throw Exception("Nodes list is empty")
        }

        val left = if (start > 0) getApproxDif(pos, nodes[start-1].position) else null
        val right = if (start < nodes.size-1) getApproxDif(pos, nodes[start+1].position) else null

        val toRight = when {
            left == null -> {
                true
            }
            right == null -> {
                false
            }
            else -> {
                left >= right
            }
        }

        val searchIds = if (toRight) start+1 until nodes.size else start-1 downTo 0

        var prevId = start
        var prev = getApproxDif(pos, nodes[prevId].position)

        for (i in searchIds) {
            val curr = getApproxDif(pos, nodes[i].position)
            if (curr >= prev) {
                break
            }
            else {
                prevId = i
                prev = curr
            }
        }
        return prevId
    }

    private fun getApproxDif(pos1: Float3, pos2: Float3): Float {
        return sqrt(+abs(pos1.x - pos2.x)
                + abs(pos1.y - pos2.y)
                + abs(pos1.z - pos2.z))
    }

}