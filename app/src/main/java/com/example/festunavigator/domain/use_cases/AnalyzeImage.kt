package com.example.festunavigator.domain.use_cases

import android.media.Image
import com.example.festunavigator.domain.ml.DetectedObjectResult
import com.example.festunavigator.domain.ml.ObjectDetector

class AnalyzeImage(
    private val objectDetector: ObjectDetector
) {
    suspend fun invoke(image: Image, imageRotation: Int): List<DetectedObjectResult>?{
        return objectDetector.analyze(image, imageRotation)
    }
}