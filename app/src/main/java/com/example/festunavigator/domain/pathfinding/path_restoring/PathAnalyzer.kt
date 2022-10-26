package com.example.festunavigator.domain.pathfinding.path_restoring

import com.example.festunavigator.data.utils.*
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toRotation

class PathAnalyzer(
    private val debug: (String) -> Unit,
    private val onChangeNeeded: (Quaternion) -> Unit,
) {

    private val directionTracker = DirectionTracker() { onNewDirection(it) }
    //Degrees
    private val maxDirectionDiff = 50
    private val minDirectionDiff = 1
    //Info about path
    private var pathSegment: List<OrientatedPosition>? = null
    private var parentQuaternion: Quaternion? = null

    fun newPosition(userPos: Float3, pathSegment: List<OrientatedPosition>, parentQuaternion: Quaternion) {
        directionTracker.newPosition(userPos)
        this.pathSegment = pathSegment
        this.parentQuaternion = parentQuaternion
    }

    private fun onNewDirection(pos: OrientatedPosition) {
        val trustQ = pos.orientation
        val path = pathSegment ?: return
        val segmentQ = path.first().orientation
        path.forEach { node ->
            if (node.orientation != segmentQ) {
                debug("Path not equal orientation")
                return
            }
        }
        val pathQ = (parentQuaternion ?: return) * (segmentQ.multiply(Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f)))
        //val pathQ = parentQuaternion * (segmentQ)
        val v = Vector3(1f, 0f, 0f)
        val trustV = trustQ.rotateVector(v)
        val pathV = pathQ.rotateVector(v)
        trustV.angleBetween(pathV).let {
            if (it > maxDirectionDiff || it < minDirectionDiff) {
                debug("Path and user big diff: $it")
                return
            }
            debug("Camera rotated. Angle diff: $it")
        }
        val t = pathQ.transition(trustQ).toRotation()
        t.x = 0f
        t.z = 0f

        onChangeNeeded(t.toQuaternion())
    }

}