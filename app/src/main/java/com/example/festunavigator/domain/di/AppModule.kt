package com.example.festunavigator.domain.di

import android.app.Application
import androidx.room.Room
import com.example.festunavigator.data.data_source.GraphDatabase
import com.example.festunavigator.data.ml.classification.TextAnalyzer
import com.example.festunavigator.data.pathfinding.AStarImpl
import com.example.festunavigator.data.repository.GraphImpl
import com.example.festunavigator.domain.ml.ObjectDetector
import com.example.festunavigator.domain.pathfinding.Pathfinder
import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.use_cases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DATABASE_NAME = "nodes"
    private const val DATABASE_DIR = "database/nodes.db"

    @Provides
    @Singleton
    fun provideDatabase(app: Application): GraphDatabase {
        return Room.databaseBuilder(app, GraphDatabase::class.java, DATABASE_NAME)
            .createFromAsset(DATABASE_DIR)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideRepository(database: GraphDatabase): GraphRepository {
        return GraphImpl(database)
    }

    @Provides
    @Singleton
    fun provideTree(repository: GraphRepository): Tree {
        return Tree(repository)
    }

    @Provides
    @Singleton
    fun provideSmoothPath(): SmoothPath {
        return SmoothPath()
    }

    @Provides
    @Singleton
    fun providePathfinder(smoothPath: SmoothPath): Pathfinder {
        return AStarImpl(smoothPath)
    }

    @Provides
    @Singleton
    fun provideFindWay(pathfinder: Pathfinder): FindWay {
        return FindWay(pathfinder)
    }

    @Provides
    @Singleton
    fun provideObjectDetector(): ObjectDetector {
        return TextAnalyzer()
    }

    @Provides
    @Singleton
    fun provideImageAnalyzer(objectDetector: ObjectDetector): AnalyzeImage {
        return AnalyzeImage(objectDetector)
    }

    @Provides
    @Singleton
    fun provideHitTest(): HitTest {
        return HitTest()
    }

    @Provides
    @Singleton
    fun provideDestinationDest(): GetDestinationDesc {
        return GetDestinationDesc()
    }

}