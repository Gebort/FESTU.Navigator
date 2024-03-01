package com.gerbort.core_ui.drawer_helper

import com.gerbort.common.di.ApplicationScope
import com.gerbort.core_ui.frame_holder.FrameConsumer
import com.gerbort.core_ui.frame_holder.FrameHolder
import com.gerbort.core_ui.frame_holder.FrameProducer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DrawerHelperModule {

    @Provides
    @Singleton
    fun provideDrawerHelper(): DrawerHelper {
        return DrawerHelperImpl()
    }

}