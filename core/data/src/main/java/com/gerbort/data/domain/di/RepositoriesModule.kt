package com.gerbort.data.domain.di

import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.data.data.repositories.RecordRepositoryImpl
import com.gerbort.data.data.repositories.TreeNodeRepositoryImpl
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.database.dao.RecordsDao
import com.gerbort.database.dao.TreeNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RepositoriesModule {

    @Provides
    @Singleton
    internal fun provideRecordRepository(
        recordsDao: RecordsDao,
        @Dispatcher(AppDispatchers.Default) dispatcher: CoroutineDispatcher
    ): RecordsRepository = RecordRepositoryImpl(recordsDao, dispatcher)

    @Provides
    @Singleton
    internal fun provideTreeNodeRepository(
        treeNodeDao: TreeNodeDao,
        @Dispatcher(AppDispatchers.Default) dispatcher: CoroutineDispatcher
    ): TreeNodeRepository = TreeNodeRepositoryImpl(treeNodeDao, dispatcher)

}