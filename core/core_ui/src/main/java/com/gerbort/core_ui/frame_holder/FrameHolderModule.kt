package com.gerbort.core_ui.frame_holder

import com.gerbort.common.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FrameHolderModule {

    @Provides
    @Singleton
    fun provideFrameHolder(
        @ApplicationScope appScope: CoroutineScope
    ): FrameHolder {
        return FrameHolder(appScope)
    }

    @Provides
    fun provideFrameConsumer(frameHolder: FrameHolder): FrameConsumer {
        return frameHolder
    }

    @Provides
    fun provideFrameProducer(frameHolder: FrameHolder): FrameProducer {
        return frameHolder
    }


}