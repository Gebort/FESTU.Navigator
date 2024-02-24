package com.gerbort.core_ui.frame_holder

import io.github.sceneview.ar.arcore.ArFrame

fun interface FrameConsumer {
    fun newFrame(frame: ArFrame)

}