package com.example.festunavigator.domain.ml

import android.media.Image
import com.example.festunavigator.data.model.DetectedObjectResult

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