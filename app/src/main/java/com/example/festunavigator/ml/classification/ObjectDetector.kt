package com.example.festunavigator.ml.classification

import android.content.Context
import android.graphics.Bitmap
import android.media.Image

import com.google.ar.core.Frame

/**
 * Describes a common interface for [GoogleCloudVisionDetector] and [MLKitObjectDetector] that can
 * infer object labels in a given [Image] and gives results in a list of [DetectedObjectResult].
 */
abstract class ObjectDetector(val context: Context) {
    private val yuvConverter = YuvToRgbConverter(context)

    /**
     * Infers a list of [DetectedObjectResult] given a camera image frame, which contains a confidence level,
     * a label, and a pixel coordinate on the image which is believed to be the center of the object.
     */
    abstract suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult>

    /**
     * [Frame.acquireCameraImage] returns an image in YUV format.
     * https://developers.google.com/ar/reference/java/com/google/ar/core/Frame#acquireCameraImage()
     *
     * Converts a YUV image to a [Bitmap] using [YuvToRgbConverter].
     */
    fun convertYuv(image: Image): Bitmap {
        return Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).apply {
            yuvConverter.yuvToRgb(image, this)
        }
    }
}