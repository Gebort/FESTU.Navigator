package com.example.festunavigator.domain.use_cases

import android.media.Image
import com.example.festunavigator.data.model.DetectedObjectResult
import com.example.festunavigator.domain.ml.ObjectDetector

class AnalyzeImage(
    private val objectDetector: ObjectDetector,
) {

    suspend operator fun invoke(
        image: Image,
        imageRotation: Int,
        imageCropPercentage: Pair<Int, Int>,
        displaySize: Pair<Int, Int>
    ): Result<DetectedObjectResult>{
        return objectDetector.analyze(
            image,
            imageRotation,
            imageCropPercentage,
            displaySize
        )
    }
}