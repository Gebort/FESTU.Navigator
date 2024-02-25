package com.gerbort.node_graph.domain.di

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.node_graph.data.adapter.NodeAdapterImpl
import com.gerbort.node_graph.data.graph.NodeGraphImpl
import com.gerbort.node_graph.domain.adapter.NodeRepositoryAdapter
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.use_cases.CreateNodeUseCase
import com.gerbort.node_graph.domain.use_cases.InitializeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
        nodeRepositoryAdapter: NodeRepositoryAdapter,
        @Dispatcher(AppDispatchers.Default) dispatcher: CoroutineDispatcher
    ): NodeGraph = NodeGraphImpl(nodeRepositoryAdapter, dispatcher)

}