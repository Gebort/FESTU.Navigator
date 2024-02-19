package com.gerbort.pathfinding.domain.di

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.pathfinding.data.AStarImpl
import com.gerbort.pathfinding.domain.PathfindUseCase
import com.gerbort.pathfinding.domain.Pathfinder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PathfindModule {

    @Provides
    @Singleton
    internal fun providePathfinder(
        @Dispatcher(AppDispatchers.Default) dispatcher: CoroutineDispatcher,
    ): Pathfinder = AStarImpl(dispatcher)

    @Provides
    @Singleton
    internal fun providePathfindUseCase(
        pathfinder: Pathfinder,
        nodeGraph: NodeGraph
    ): PathfindUseCase = PathfindUseCase(nodeGraph, pathfinder)

}