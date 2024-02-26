package com.gerbort.pathfinding.domain.di

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.ApplicationScope
import com.gerbort.common.di.Dispatcher
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.use_cases.GetEntryUseCase
import com.gerbort.pathfinding.data.manager.PathManagerImpl
import com.gerbort.pathfinding.domain.Pathfinder
import com.gerbort.pathfinding.domain.manager.PathManager
import com.gerbort.pathfinding.domain.use_cases.PathfindUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PathManagerModule {

    @Provides
    @Singleton
    internal fun providePathManager(
        @ApplicationScope applicationScope: CoroutineScope,
        getEntryUseCase: GetEntryUseCase,
        pathfindUseCase: PathfindUseCase,
        recordsRepository: RecordsRepository
    ): PathManager = PathManagerImpl(pathfindUseCase, getEntryUseCase, recordsRepository, applicationScope)

}