package com.gerbort.node_graph.domain.di

import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.node_graph.data.adapter.NodeAdapterImpl
import com.gerbort.node_graph.data.graph.NodeGraphImpl
import com.gerbort.node_graph.domain.adapter.NodeRepositoryAdapter
import com.gerbort.node_graph.domain.graph.NodeGraph
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NodeGraphModule {

    @Provides
    @Singleton
    internal fun provideNodeRepositoryAdapter(
        treeNodeRepository: TreeNodeRepository
    ): NodeRepositoryAdapter = NodeAdapterImpl(treeNodeRepository)

    @Provides
    @Singleton
    internal fun provideNodeGraph(
        nodeRepositoryAdapter: NodeRepositoryAdapter
    ): NodeGraph = NodeGraphImpl(nodeRepositoryAdapter)

}