package com.gerbort.smoothing

import com.gerbort.common.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import javax.inject.Singleton

@Module
@InstallIn(ApplicationScope::class)
internal object SmoothingModule {

    @Provides
    @Singleton
    internal fun provideSmoothWayUseCase(): SmoothWayUseCase {
        return SmoothWayUseCase()
    }

}