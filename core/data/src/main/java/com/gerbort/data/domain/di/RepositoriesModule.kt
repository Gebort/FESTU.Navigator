package com.gerbort.data.domain.di

import android.content.Context
import com.gerbort.data.data.repositories.RecordRepositoryImpl
import com.gerbort.data.data.repositories.TreeNodeRepositoryImpl
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.data.domain.repositories.TreeNodeRepository
import com.gerbort.database.AppDatabase
import com.gerbort.database.dao.RecordsDao
import com.gerbort.database.dao.TreeNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RepositoriesModule {

    @Provides
    @Singleton
    internal fun provideRecordRepository(
        recordsDao: RecordsDao
    ): RecordsRepository = RecordRepositoryImpl(recordsDao)

    @Provides
    @Singleton
    internal fun provideTreeNodeRepository(
        treeNodeDao: TreeNodeDao
    ): TreeNodeRepository = TreeNodeRepositoryImpl(treeNodeDao)

}