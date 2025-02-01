package com.gerbort.core_ui.azimuth_holder

import kotlinx.coroutines.flow.Flow

interface AzimuthProducer {
    fun getAzimuthRadians(): Flow<Double>
}