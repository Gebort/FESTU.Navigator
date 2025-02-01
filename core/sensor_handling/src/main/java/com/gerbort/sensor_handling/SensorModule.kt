package com.gerbort.sensor_handling

import com.gerbort.core_ui.azimuth_holder.AzimuthConsumer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SensorModule {

    @Provides
    @Singleton
    internal fun provideSensorHandler(
        azimuthConsumer: AzimuthConsumer,
    ): SensorHandler = SensorHandler(azimuthConsumer)

}