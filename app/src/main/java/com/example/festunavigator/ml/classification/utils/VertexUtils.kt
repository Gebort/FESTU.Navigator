package com.example.festunavigator.ml.classification.utils

import com.example.festunavigator.common.samplerender.arcore.NormalizedVertex

object VertexUtils {
    /**
     * Convert a [NormalizedVertex] to an absolute coordinate pair.
     */
    fun NormalizedVertex.toAbsoluteCoordinates(
        imageWidth: Int,
        imageHeight: Int,
    ): Pair<Int, Int> {
        return (x * imageWidth).toInt() to (y * imageHeight).toInt()
    }

    /**
     * Rotates a coordinate pair according to [imageRotation].
     */
    fun Pair<Int, Int>.rotateCoordinates(
        imageWidth: Int,
        imageHeight: Int,
        imageRotation: Int,
    ): Pair<Int, Int> {
        val (x, y) = this
        return when (imageRotation) {
            0 -> x to y
            180 -> imageWidth - x to imageHeight - y
            90 -> y to imageWidth - x
            270 -> imageHeight - y to x
            else -> error("Invalid imageRotation $imageRotation")
        }
    }

    /**
     * Calculate a point using the average of points in the bounding polygon.
     */
    fun List<NormalizedVertex>.calculateAverage(): NormalizedVertex {
        var averageX = 0f
        var averageY = 0f
        for (vertex in this) {
            averageX += vertex.x / size
            averageY += vertex.y / size
        }
        return NormalizedVertex(averageX, averageY)
    }

}