package com.example.festunavigator.presentation.preview.navigation

import com.gerbort.initialization.navigation.OrientationNavigator
import com.gerbort.router.navigation.RouterNavigator
import com.gerbort.scanner.navigation.ScannerNavigator
import com.gerbort.search.navigation.SearchNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    internal fun provideAppNavigator(): AppNavigator = AppNavigator()

    @Provides
    @Singleton
    internal fun provideOrientationNavigator(appNavigator: AppNavigator): OrientationNavigator {
        return appNavigator
    }

    @Provides
    @Singleton
    internal fun provideScannerNavigator(appNavigator: AppNavigator): ScannerNavigator {
        return appNavigator
    }

    @Provides
    @Singleton
    internal fun provideRouterNavigator(appNavigator: AppNavigator): RouterNavigator {
        return appNavigator
    }

    @Provides
    @Singleton
    internal fun provideSearchNavigator(appNavigator: AppNavigator): SearchNavigator {
        return appNavigator
    }



}