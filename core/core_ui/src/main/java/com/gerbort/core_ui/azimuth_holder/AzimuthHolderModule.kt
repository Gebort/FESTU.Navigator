package com.gerbort.core_ui.azimuth_holder

import com.gerbort.common.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AzimuthHolderModule {

    @Provides
    @Singleton
    fun provideAzimuthHolder(
        @ApplicationScope appScope: CoroutineScope
    ): AzimuthHolder {
        return AzimuthHolder (appScope)
    }

    @Provides
    fun provideAzimuthConsumer(azimuthHolder: AzimuthHolder): AzimuthConsumer {
        return azimuthHolder
    }

    @Provides
    fun provideAzimuthProducer(azimuthHolder: AzimuthHolder): AzimuthProducer {
        return azimuthHolder
    }


}