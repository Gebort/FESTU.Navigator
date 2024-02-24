package com.gerbort.core_ui.frame_holder

import com.gerbort.common.di.ApplicationScope
import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class FrameHolder(
    @ApplicationScope private val appScope: CoroutineScope
): FrameConsumer, FrameProducer {

    private var _frame = MutableStateFlow<ArFrame?>(null)

    override fun newFrame(frame: ArFrame) {
        appScope.launch {
            _frame.emit(frame)
        }
    }

    override fun getFrames(): Flow<ArFrame?> = _frame.asStateFlow()


}