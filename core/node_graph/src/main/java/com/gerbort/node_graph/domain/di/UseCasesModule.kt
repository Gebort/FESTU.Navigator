package com.gerbort.node_graph.domain.di

import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.use_cases.CreateNodeUseCase
import com.gerbort.node_graph.domain.use_cases.DeleteNodeUseCase
import com.gerbort.node_graph.domain.use_cases.GetEntryUseCase
import com.gerbort.node_graph.domain.use_cases.InitializeUseCase
import com.gerbort.node_graph.domain.use_cases.LinkNodesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UseCasesModule {

    @Provides
    @Singleton
    internal fun provideInitializeUseCase(
        nodeGraph: NodeGraph
    ): InitializeUseCase = InitializeUseCase(nodeGraph)

    @Provides
    @Singleton
    internal fun provideCreateNodeUseCase(
        nodeGraph: NodeGraph
    ): CreateNodeUseCase = CreateNodeUseCase(nodeGraph)

    @Provides
    @Singleton
    internal fun provideDeleteNodeUseCase(
        nodeGraph: NodeGraph
    ): DeleteNodeUseCase = DeleteNodeUseCase(nodeGraph)

    @Provides
    @Singleton
    internal fun linkNodesUseCase(
        nodeGraph: NodeGraph
    ): LinkNodesUseCase = LinkNodesUseCase(nodeGraph)

    @Provides
    @Singleton
    internal fun getEntryUseCase(
        nodeGraph: NodeGraph
    ): GetEntryUseCase = GetEntryUseCase(nodeGraph)

}