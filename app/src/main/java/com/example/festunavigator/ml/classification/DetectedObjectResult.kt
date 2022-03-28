package com.example.festunavigator.ml.classification

/**
 * A [DetectedObjectResult] describes a single result in a labeled image.
 * @property confidence The model's reported confidence for this inference result (normalized over `[0, 1]`).
 * @property label The model's reported label for this result.
 * @property centerCoordinate A point on the image that best describes the object's location.
 */
data class DetectedObjectResult(
    val confidence: Float,
    val label: String,
    val centerCoordinate: Pair<Int, Int>
)