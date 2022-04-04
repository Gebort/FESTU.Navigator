package com.example.festunavigator.data.ml.classification.utils


object VertexUtils {

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

}