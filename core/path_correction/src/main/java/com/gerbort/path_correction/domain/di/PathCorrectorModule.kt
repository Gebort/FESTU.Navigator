package com.gerbort.path_correction.domain.di

import com.gerbort.path_correction.data.PathAnalyzer
import com.gerbort.path_correction.domain.PathCorrector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object PathCorrectorModule {

    @Provides
    fun providePathCorrector(): PathCorrector {
        return PathAnalyzer()
    }

}