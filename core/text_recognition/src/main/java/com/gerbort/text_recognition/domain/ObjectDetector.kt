package com.gerbort.text_recognition.domain

import android.media.Image

/**
 * Describes a common interface for [GoogleCloudVisionDetector] and [MLKitObjectDetector] that can
 * infer object labels in a given [Image] and gives results in a list of [DetectedObjectResult].
 */
interface ObjectDetector {

    /**
     * Infers a list of [DetectedObjectResult] given a camera image frame, which contains a confidence level,
     * a label, and a pixel coordinate on the image which is believed to be the center of the object.
     */
    suspend fun analyze(
        mediaImage: Image,
        rotationDegrees: Int,
        imageCropPercentages: Pair<Int, Int>,
        displaySize: Pair<Int, Int>
    ): Result<DetectedObjectResult>
}

sealed class DetectorException(msg: String): Exception(msg) {
    data object NoBoundingBox: DetectorException("Cant detect bounding box")
    data object NoDigits: DetectorException("No digits found")
    data object NoObjects: DetectorException("No detected objects")
    data object NullText: DetectorException("Null text")
}