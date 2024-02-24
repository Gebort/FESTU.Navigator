package com.gerbort.text_recognition.domain

import android.media.Image
import javax.inject.Inject

class DetectTextUseCase @Inject constructor (
    private val objectDetector: ObjectDetector,
) {

    suspend operator fun invoke(
        image: Image,
        imageRotation: Int,
        displaySize: Pair<Int, Int>,
        imageCropPercentage: Pair<Int, Int> = Pair(8, 72)
    ): Result<DetectedObjectResult>{
        return objectDetector.analyze(
            image,
            imageRotation,
            imageCropPercentage,
            displaySize
        )
    }
}