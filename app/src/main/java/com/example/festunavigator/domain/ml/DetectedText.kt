package com.example.festunavigator.domain.ml

import com.example.festunavigator.data.model.DetectedObjectResult
import com.google.ar.core.Frame

data class DetectedText(
    val detectedObjectResult: DetectedObjectResult,
    val frame: Frame
)
