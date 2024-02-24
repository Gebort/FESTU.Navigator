package com.gerbort.core_ui.frame_holder

import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.flow.Flow

fun interface FrameProducer {

    fun getFrames(): Flow<ArFrame?>

}