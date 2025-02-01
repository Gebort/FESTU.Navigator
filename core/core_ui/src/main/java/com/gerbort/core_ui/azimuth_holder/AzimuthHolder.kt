package com.gerbort.core_ui.azimuth_holder

import com.gerbort.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AzimuthHolder(
    @ApplicationScope private val appScope: CoroutineScope
): AzimuthConsumer, AzimuthProducer {

    private val _azimuth = MutableSharedFlow<Double>()

    override fun newAzimuth(azimuthRadians: Double) {
        appScope.launch {
            _azimuth.emit(azimuthRadians)
        }
    }

    override fun getAzimuthRadians(): Flow<Double> {
        return _azimuth.asSharedFlow()
    }
}