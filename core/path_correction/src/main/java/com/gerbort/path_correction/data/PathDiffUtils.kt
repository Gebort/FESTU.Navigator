package com.gerbort.path_correction.data

import com.gerbort.common.model.OrientatedPosition
import com.gerbort.common.model.Path
import com.gerbort.common.utils.getApproxDif
import dev.romainguy.kotlin.math.Float3

class PathDiffUtils(
    private val path: Path
) {

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
        if (right > path.points.size - 1) {
            right = path.points.size - 1
        }

        return List(right - left + 1) {
            path.points[it + left]
        }
    }

    private fun linearNodeSearch(pos: Float3, start: Int = 0): Int {
        if (path.points.isEmpty()){
            throw Exception("Nodes list is empty")
        }

        val left = if (start > 0) pos.getApproxDif(path.points[start-1].position) else null
        val right = if (start < path.points.size-1) pos.getApproxDif(path.points[start+1].position) else null

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

        val searchIds = if (toRight) start+1 until path.points.size else start-1 downTo 0

        var prevId = start
        var prev = pos.getApproxDif(path.points[prevId].position)

        for (i in searchIds) {
            val curr = pos.getApproxDif(path.points[i].position)
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
}