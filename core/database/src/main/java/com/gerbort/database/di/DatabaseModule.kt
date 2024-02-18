package com.gerbort.database.di

import android.content.Context
import androidx.room.Room
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
internal object DatabaseModule {

    @Provides
    @Singleton
    internal fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app-database",
    ).build()

    @Provides
    internal fun provideTreeNodeDao(
        database: AppDatabase
    ): TreeNodeDao = database.treeNodeDao

    @Provides
    internal fun provideRecordsDao(
        database: AppDatabase
    ): RecordsDao = database.recordsDao

}