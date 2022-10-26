package com.example.festunavigator.domain.pathfinding.path_restoring

import com.example.festunavigator.data.utils.angleBetween
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.data.utils.fromVector
import com.example.festunavigator.domain.utils.getApproxDif
import com.example.festunavigator.domain.utils.meanPoint
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.*
import kotlin.math.cos
import kotlin.math.sin

class DirectionTracker(
    private val debug: (String) -> Unit,
    private val onNewDirection: (OrientatedPosition) -> Unit
) {

    //Minimum position difference to count the point as "non idle"
    private val idleDiff = 0.08
    //Maximum length between direction vector and a new point, to continue the vector
    private val directionDiff = 0.1
    //Minimum length for vector to change path coordinates
    private val lengthThreshold = 1
    //Minimum points amount to calculate initial direction vector
    private val minPoints = 20

    private var direction: Segment? = null
    private val lastPoints: MutableList<Position> = mutableListOf()

    fun newPosition(pos: Position) {
        if (lastPoints.isNotEmpty() && lastPoints.last().getApproxDif(pos) < idleDiff) {
            return
        }
        if (lastPoints.size >= minPoints) {
            lastPoints.removeFirst()
        }
        lastPoints.add(pos)
        debug(lastPoints.size.toString())

        if (lastPoints.size >= minPoints && direction == null) {
            direction = approximateSegment(lastPoints)
            checkForCallback()
            return
        }

        direction?.let {
            val pVector = (pos - it.startPos).toVector3()
            val angle = pVector.angleBetween(it.vector)
            val diff = pVector.length() * sin(angle)
            if (diff <= directionDiff && angle < 90) {
                val newLength = pVector.length() * cos(angle)
                if (newLength > it.vector.length()) {
                    direction = it.copy(
                        vector = it.vector.scaled(newLength / it.vector.length())
                    )
                    checkForCallback()
                }
                return
            }
            else {
                lastPoints.clear()
                direction = null
                //direction = approximateSegment(lastPoints)
            }

        }
    }

    private fun checkForCallback() {
        direction?.let {
            if (it.vector.length() >= lengthThreshold) {
                onNewDirection(
                    OrientatedPosition(
                        position = it.startPos.meanPoint(it.endPos),
                        orientation = Quaternion.fromVector(it.vector)
                    )
                )
            }
        }
    }

    private fun approximateSegment(points: List<Position>): Segment {
        val xList = mutableListOf<Float>()
        val zList = mutableListOf<Float>()
        for ((x, _, z) in points) {
            xList.add(x)
            zList.add(z)
        }
        val model = LinearRegressionModel(
            independentVariables = xList,
            dependentVariables = zList
        )

        val z1 = model.predict(xList.first())
        val z2 = model.predict(xList.last())

        val p1 = Position(xList.first(), 0f, z1)
        val p2 = Position(xList.last(), 0f, z2)

        return Segment(
            startPos = p1,
            vector =  (p2 - p1).toVector3()
        )
    }

}

//represents a vector but with a start and an end points
//same as OrientatedPosition, but using vectors instead of quaternions
data class Segment(
    val startPos: Position,
    val vector: Vector3
) {
    val endPos get() = startPos + vector.toFloat3()

}


