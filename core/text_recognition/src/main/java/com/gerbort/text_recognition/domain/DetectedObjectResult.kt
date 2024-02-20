package com.gerbort.text_recognition.domain

import com.google.ar.core.Frame
import dev.romainguy.kotlin.math.Float2

data class DetectedText(
    val detectedObjectResult: DetectedObjectResult,
    val frame: Frame
)

data class DetectedObjectResult(
    val label: String,
    val centerCoordinate: Float2,
)