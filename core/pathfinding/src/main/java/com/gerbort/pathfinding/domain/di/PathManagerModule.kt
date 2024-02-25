package com.gerbort.pathfinding.domain.di

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.pathfinding.data.manager.PathManagerImpl
import com.gerbort.pathfinding.domain.Pathfinder
import com.gerbort.pathfinding.domain.manager.PathManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PathManagerModule {

    @Provides
    @Singleton
    internal fun providePathManager(
        @Dispatcher(AppDispatchers.Default) dispatcher: CoroutineDispatcher,
        nodeGraph: NodeGraph,
        pathfinder: Pathfinder
    ): PathManager = PathManagerImpl(nodeGraph, pathfinder, dispatcher)

}