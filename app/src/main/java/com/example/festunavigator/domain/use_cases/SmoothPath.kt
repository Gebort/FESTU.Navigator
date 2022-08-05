package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.smoothing.BezierPoint
import com.example.festunavigator.domain.tree.TreeNode
import com.google.ar.sceneform.math.Vector3
import dev.benedikt.math.bezier.curve.BezierCurve
import dev.benedikt.math.bezier.curve.Order
import dev.benedikt.math.bezier.math.DoubleMathHelper
import dev.benedikt.math.bezier.vector.Vector3D
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toVector3

private const val ROUTE_STEP = 0.3f

class SmoothPath {

    operator fun invoke(
        nodes: List<TreeNode>
    ): List<OrientatedPosition> {

        val list = mutableListOf<OrientatedPosition>()

        if (nodes.size > 3){
            for (i in 1 until nodes.size-2) {
                val node1 = nodes[i]
                val node2 = nodes[i+1]

                val fromVector = node1.position.toVector3()
                val toVector = node2.position.toVector3()

                val lineLength = Vector3.subtract(fromVector, toVector).length()

                if (lineLength < ROUTE_STEP){
                    continue
                }

                val nodesAmount = (lineLength / ROUTE_STEP).toInt()

                val dx = (toVector.x - fromVector.x) / nodesAmount
                val dy = (toVector.y - fromVector.y) / nodesAmount
                val dz = (toVector.z - fromVector.z) / nodesAmount

                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB: com.google.ar.sceneform.math.Quaternion =
                    com.google.ar.sceneform.math.Quaternion.lookRotation(
                        directionFromTopToBottom,
                        Vector3.up()
                    )

                val rotation = com.google.ar.sceneform.math.Quaternion.multiply(
                    rotationFromAToB,
                    com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                ).toNewQuaternion()

                val lowStep =  if (nodesAmount < 3) 0 else if (nodesAmount == 3) 1 else 2
                val highStep = if (nodesAmount < 3) 1 else if (nodesAmount == 3) 2 else 3
                for (j in 0..nodesAmount-highStep) {
                    val position = Float3(
                        fromVector.x + dx * j,
                        fromVector.y + dy * j,
                        fromVector.z + dz * j
                    )

                    if (list.isEmpty() || i == 1){
                        val pos = OrientatedPosition(position, rotation)
                        list.add(pos)
                    }
                    else if (j == lowStep) {
                        list.addAll(
                            getBezierSmoothPoints(
                                list.removeLast().position,
                                position,
                                fromVector.toFloat3(),
                                ROUTE_STEP
                            )
                        )
                    }
                    else if (j > lowStep) {
                        val pos = OrientatedPosition(position, rotation)
                        list.add(pos)

                    }
                }

                if (i == nodes.size-3){
                    for (j in nodesAmount - highStep until nodesAmount) {
                        val position = Float3(
                            fromVector.x + dx * j,
                            fromVector.y + dy * j,
                            fromVector.z + dz * j
                        )
                        val pos = OrientatedPosition(position, rotation)
                        list.add(pos)
                    }
                }
            }

        }

       return list
    }

    private fun getBezierSmoothPoints(
        start: Float3,
        end: Float3,
        point: Float3,
        step: Float
    ): List<OrientatedPosition> {

        val curve = bezierCurve(
            start,
            end,
            point
        )

        val curveLen = curve.length
        val rawPointsAmount = 50
        val rawStep = curveLen/rawPointsAmount
        val rawPoints = mutableListOf<BezierPoint>()

        var i = 0.0
        while (i < curveLen) {
            val t = i/curveLen
            val pos = curve.getCoordinatesAt(t)
            rawPoints.add(BezierPoint(t, pos))
            i += rawStep
        }
        val endPoint = BezierPoint(1.0, curve.getCoordinatesAt(1.0))

        return walkCurve(endPoint, rawPoints, step.toDouble()).map { rawPoint ->
            val pos = rawPoint.pos.toFloat3()
            val tangent = curve.getTangentAt(rawPoint.t)
            pos.x = pos.x
            tangent.y = tangent.y
            OrientatedPosition(
                rawPoint.pos.toFloat3(),
                Quaternion.lookRotation(curve.getTangentAt(rawPoint.t).toVector3())
            )

        }

    }

    private fun bezierCurve(
        start: Float3,
        end: Float3,
        point: Float3
    ): BezierCurve<Double, Vector3D> {

        val curve = BezierCurve(
            Order.QUADRATIC,
            start.toVector3D(),
            end.toVector3D(),
            listOf(point.toVector3D()),
            20,
            DoubleMathHelper()
        )

        curve.computeLength()

        return curve
    }

    private fun walkCurve(
        end: BezierPoint,
        points: List<BezierPoint>,
        spacing: Double,
        offset: Double = 0.0
    ): List<BezierPoint>
    {
        val result = mutableListOf<BezierPoint>()

        val space= if (spacing > 0.00001) spacing else 0.00001;

        var distanceNeeded = offset
        while (distanceNeeded < 0)
        {
            distanceNeeded += space;
        }

        var current = points[0]
        var next = points[1]
        var i = 1
        val last = points.count() - 1
        while (true)
        {
            val diff = next.pos - current.pos
            val dist = diff.magnitude()

            if (dist >= distanceNeeded)
            {
                current.pos += diff * (distanceNeeded / dist)
                result.add(current)
                distanceNeeded = spacing
            }
            else if (i != last)
            {
                distanceNeeded -= dist
                current = next
                next = points[++i]
            }
            else
            {
                break
            }
        }

        val dist = (result.last().pos - end.pos).magnitude()
        if (dist < spacing / 2) {
            result.removeLast()
            result.add(end)
        }
        else {
            result.add(end)
        }

        return result
    }

    private fun Float3.toVector3D(): Vector3D {
        return Vector3D(
            this.x.toDouble(),
            this.y.toDouble(),
            this.z.toDouble()
        )
    }

    private fun Vector3D.toFloat3(): Float3 {
        return Float3(
            this.x.toFloat(),
            this.y.toFloat(),
            this.z.toFloat()
        )
    }

    private fun Vector3D.toVector3(): Vector3 {
        return Vector3(
            this.x.toFloat(),
            this.y.toFloat(),
            this.z.toFloat()
        )
    }

}