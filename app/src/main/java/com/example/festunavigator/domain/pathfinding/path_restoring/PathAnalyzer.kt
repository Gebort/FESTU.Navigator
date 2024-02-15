package com.example.festunavigator.domain.pathfinding.path_restoring

import com.example.festunavigator.data.utils.*
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.tree.TreeNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.schemas.lull.Quat
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toRotation
import io.github.sceneview.math.toVector3
import kotlin.math.cos

class PathAnalyzer(
    private val debug: (String, Int) -> Unit,
    private val onChangeNeeded: (Quaternion) -> Unit,
) {

    private val directionTracker = DirectionTracker(debug) { onNewDirection(it) }
    private var lastUserDirection: OrientatedPosition? = null
    //Degrees
    private val maxDirectionDiff = 50
    private val minDirectionDiff = 1
    //Min path segment length
    private val minLength = 0.0f
    //Info about path segment - start and end
    private var pathSegment: Pair<TreeNode, TreeNode>? = null
    //Info about TreeAdapter parent node quaternion
    private var parentQuaternion: Quaternion? = null
    //Info about current north position
    private var northPosition: Float3? = null

    fun newPosition(
        userPos: Float3,
        northPosition: Float3?,
        pathSegment: Pair<TreeNode,TreeNode>?,
        parentQuaternion: Quaternion?,
    ) {
        directionTracker.newPosition(userPos)
        this.pathSegment = pathSegment
        this.parentQuaternion = parentQuaternion
        this.northPosition = northPosition
    }

    private fun compareDirection(
        pathSegment: Pair<TreeNode, TreeNode>,
        northPosition: Float3
    ) {

        val node = if (pathSegment.first.northDirection != null) pathSegment.first else pathSegment.second
//        node.northDirection?.let { oldDirection ->
//            val northVector = northPosition - node.position
//            val northQuaternion = northVector.toQuaternion()
//            val transition = oldDirection.transition(northQuaternion)
//            transition.to
//        }


    }

    private fun onNewDirection(pos: OrientatedPosition) {
        if (pos == lastUserDirection) return
        lastUserDirection = pos
        val trustQ = pos.orientation
        val pathStart = pathSegment?.first ?: return
        val pathEnd = pathSegment?.second ?: return
        val segmentQ = Quaternion.fromVector((pathEnd.position - pathStart.position).toVector3())
        val pathQ = (parentQuaternion ?: return) * segmentQ
        val v = Vector3(1f, 0f, 0f)
        val trustV = trustQ.rotateVector(v)
        val pathV = pathQ.rotateVector(v)

        checkVectorLength(
            pathStart.position,
            (pathEnd.position - pathStart.position).toVector3(),
            pos.position,
            minLength
        ).let {
            if(!it) {
                    debug("Path segment small length", 1)
                    return
            }
        }

        var reverse = false
        val angle = pathV.angleBetween(trustV).let {
            if (it > 90f) {
                reverse = true
                180 - it
            } else it
        }
        angle.let {
            if (it > maxDirectionDiff || it < minDirectionDiff) {
                debug("Path and user big diff: $it", 1)
                return
            }
            debug("Camera rotated. Angle diff: $it", 1)
        }

        val t = pathQ.transition(trustQ).toRotation()
        t.x = 0f
        t.z = 0f
        onChangeNeeded(if (reverse) t.toQuaternion().inverted() else t.toQuaternion())
    }

    private fun checkVectorLength(origin: Float3, vector: Vector3, point: Float3, minLength: Float): Boolean {
        val pVector = (point - origin).toVector3()
        val angle = vector.angleBetween(pVector)
        val c = cos(Math.toRadians(angle.toDouble()))
        val l = pVector.length()
        val leftBase = c * l
        val minPart = minLength/2
        if (leftBase < minPart) {
            return false
        }
        if (vector.length() - leftBase < minPart) return false
        return true

    }

}