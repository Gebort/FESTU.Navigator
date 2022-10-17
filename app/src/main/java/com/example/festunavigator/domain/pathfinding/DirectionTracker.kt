package com.example.festunavigator.domain.pathfinding

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.use_cases.fromVector
import com.example.festunavigator.domain.utils.getApproxDif
import com.example.festunavigator.domain.utils.meanPoint
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.angleBetweenVectors
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.*
import kotlin.math.cos

class DirectionTracker(
    private val initialPos: Position
) {


    val directions: MutableList<Segment> = mutableListOf()

    private var lastPos: Position? = null

    private val idleDiff = 0.01
    //Degrees
    private val directionDiff = 10
    //Minimum length for vector to change path coordinates
    private val lengthThreshold = 1

    fun getStraightOrientation(): OrientatedPosition? {
        if (directions.isEmpty()) {
            return null
        }
        directions.last().let { d ->
            if (d.vector.length() >= lengthThreshold) {
                return OrientatedPosition(
                    position = d.startPos.meanPoint(d.endPos),
                    orientation = Quaternion.fromVector(d.vector)
                )
            }
            else {
                return null
            }
        }

    }

    fun newPosition(pos: Position) {
        if (lastPos != null && lastPos?.getApproxDif(pos)!! < idleDiff) {
            return
        }
        if (lastPos == null) {
            lastPos = pos
            directions.add(Segment(
                startPos = initialPos,
                vector = (pos - initialPos).toVector3())
            )
            return
        }

        val startPos = directions.last().startPos
        val testVector = (pos - startPos).toVector3()
        angleBetweenVectors(directions.last().vector, testVector).let {
            directions.last().let { lastSegment ->
                if (it < directionDiff) {
                    val newLength = testVector.length() * cos(it)

                    directions[directions.lastIndex] = lastSegment.copy(
                        vector = lastSegment.vector.scaled(newLength / lastSegment.vector.length())
                    )
                } else {
                    val newStartPos = lastSegment.endPos
                    val newVector = (pos - newStartPos).toVector3()
                    directions.add(
                        Segment(
                            startPos = newStartPos,
                            vector = newVector
                        )
                    )
                }
            }
        }
        lastPos = pos
    }

}

//represents a vector but with a start and end points
//same as OrientatedPosition, but using vectors instead of quaternions
data class Segment(
    val startPos: Position,
    val vector: Vector3
) {
    val endPos get() = startPos + vector.toFloat3()
}


