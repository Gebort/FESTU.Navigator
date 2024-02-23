package com.example.festunavigator.data.path_restoring

import com.example.festunavigator.domain.utils.getApproxDif
import com.example.festunavigator.domain.utils.meanPoint
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.*
import kotlin.math.cos
import kotlin.math.sin

class DirectionTracker(
    private val debug: (String, Int) -> Unit,
    private val onNewDirection: (OrientatedPosition) -> Unit
) {

    //Minimum position difference to count the point as "non idle"
    private val idleDiff = 0.08
    //Maximum length between direction vector and a new point, to continue the vector
    private val directionDiff = 0.8
    //Minimum length for vector to change path coordinates
    private val lengthThreshold = 1
    //Minimum points amount to calculate initial direction vector
    private val minPoints = 20
    //Minimum R^2 to use direction predicted with LinearRegressionModel
    private val r2Threshold = 0.6

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
        debug(lastPoints.size.toString(), 2)

        if (lastPoints.size >= minPoints && direction == null) {
            direction = approximateSegment(lastPoints)
            if (direction == null) {
                lastPoints.clear()
            }
            else {
                checkForCallback()
            }
            return
        }

        direction?.let {
            val pVector = (pos - it.startPos).toVector3().apply {
                y = 0f
            }
            val angle = pVector.angleBetween(it.vector)
            val h = pVector.length() * sin(Math.toRadians(angle.toDouble()))
            val newBase = pVector.length() * cos(angle)
            val oldBase = it.vector.length()
            if (h <= directionDiff && newBase > oldBase) {
                direction = it.copy(
                    vector = it.vector.scaled(newBase / oldBase)
                )
                checkForCallback()
                return
            }
            else {
                if (h > directionDiff) {
                    debug("Cleared, big h $h", 1)
                }
                if (newBase > oldBase) debug("Cleared, reverse direction", 1)
                lastPoints.clear()
                direction = null
            }

        }
    }

    private fun checkForCallback() {
        direction?.let {
            lastPoints.lastOrNull()?.let { lastPoint ->
                if (it.vector.length() >= lengthThreshold) {
                    onNewDirection(
                        OrientatedPosition(
                            position = lastPoint,
                            orientation = Quaternion.fromVector(it.vector)
                        )
                    )
                }
            }
        }
    }

    private fun approximateSegment(points: List<Position>): Segment? {
        val xList = mutableListOf<Float>()
        val zList = mutableListOf<Float>()
        val xTest = mutableListOf<Float>()
        val zTest = mutableListOf<Float>()
        points.forEachIndexed { index, (x, _, z) ->
            if (index % 3 == 0){
                xTest.add(x)
                zTest.add(z)
            }
            else {
                xList.add(x)
                zList.add(z)
            }

        }
        val model = LinearRegressionModel(
            independentVariables = xList,
            dependentVariables = zList
        )

        val r2 = model.test(
            xTest = xTest,
            yTest = zTest
        )

        if (r2 < r2Threshold) {
            debug("Bad r^2: $r2", 1)
            return null
        }

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


