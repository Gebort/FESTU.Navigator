package com.gerbort.hit_test

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HitTestModule {

    @Provides
    @Singleton
    internal fun provideHitTest(): HitTestUseCase {
        return HitTestUseCase()
    }

}