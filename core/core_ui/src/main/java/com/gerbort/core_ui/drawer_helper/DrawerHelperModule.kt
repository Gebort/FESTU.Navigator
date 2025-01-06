package com.gerbort.core_ui.drawer_helper

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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