package com.gerbort.text_recognition.domain

import android.media.Image
import javax.inject.Inject

class DetectTextUseCase @Inject constructor (
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