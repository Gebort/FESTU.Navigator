package com.example.festunavigator.domain.pathfinding.path_restoring

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.use_cases.transition
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toRotation

class PathAnalyzer(
    private val initialPos: Float3,
    private val onChangeNeeded: (Quaternion) -> Unit
) {

    private val directionTracker = DirectionTracker(initialPos)
    //Degrees
    private val directionDiff = 10

    fun newPosition(userPos: Float3, pathSegment: List<OrientatedPosition>) {
        directionTracker.newPosition(userPos)
        val trustyQ = directionTracker.getStraightOrientation()?.orientation ?: return
        val pathQ = pathSegment.first().orientation
        pathSegment.forEach { node ->
            if (node.orientation != pathQ) {
                return
            }
        }
        val trustyAngles = trustyQ.toRotation()
        val pathAngles = pathQ.toRotation()
        if (trustyAngles.x - pathAngles.x > directionDiff) {
            return
        }
        if (trustyAngles.z - pathAngles.z > directionDiff){
            return
        }
        onChangeNeeded(pathQ.transition(trustyQ))
    }

}