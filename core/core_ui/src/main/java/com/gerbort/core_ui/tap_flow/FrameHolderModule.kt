package com.gerbort.core_ui.tap_flow

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
    fun provideTapHolder(
        @ApplicationScope appScope: CoroutineScope
    ): UserTapHolder {
        return UserTapHolder(appScope)
    }

    @Provides
    fun provideTapConsumer(tapHolder: UserTapHolder): UserTapConsumer {
        return tapHolder
    }

    @Provides
    fun provideFrameProducer(tapHolder: UserTapHolder): UserTapProducer {
        return tapHolder
    }


}