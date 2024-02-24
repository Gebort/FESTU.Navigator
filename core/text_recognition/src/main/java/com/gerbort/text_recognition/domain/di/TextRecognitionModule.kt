package com.gerbort.text_recognition.domain.di

import com.gerbort.text_recognition.data.TextAnalyzer
import com.gerbort.text_recognition.domain.DetectTextUseCase
import com.gerbort.text_recognition.domain.DetectedObjectResult
import com.gerbort.text_recognition.domain.ObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TextRecognitionModule {

    @Provides
    @Singleton
    internal fun provideObjectDetector(): ObjectDetector {
        return TextAnalyzer()
    }

    @Provides
    @Singleton
    internal fun provideDetectTextUseCase(
        objectDetector: ObjectDetector
    ): DetectTextUseCase {
        return DetectTextUseCase(objectDetector)
    }

}