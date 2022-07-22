package com.example.festunavigator.data

import android.app.Application
import androidx.room.Room
import com.example.festunavigator.data.data_source.GraphDatabase
import com.example.festunavigator.data.ml.classification.TextAnalyzer
import com.example.festunavigator.data.pathfinding.AStarImpl
import com.example.festunavigator.data.repository.GraphImpl
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.use_cases.*

class App : Application() {
    private lateinit var database: GraphDatabase

    private lateinit var repository: GraphImpl
    private lateinit var tree: Tree

    lateinit var findWay: FindWay

    private lateinit var pathfinder: AStarImpl

    lateinit var hitTest: HitTest

    private lateinit var objectDetector: TextAnalyzer
    lateinit var analyzeImage: AnalyzeImage

    lateinit var getDestinationDesc: GetDestinationDesc

    lateinit var smoothPath: SmoothPath

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, GraphDatabase::class.java, DATABASE_NAME)
            .createFromAsset(DATABASE_DIR)
            .allowMainThreadQueries()
            .build()
        repository = GraphImpl()
        tree = Tree(repository)

        smoothPath = SmoothPath()

        pathfinder = AStarImpl()
        findWay = FindWay(pathfinder)

        hitTest = HitTest()

        objectDetector = TextAnalyzer()
        analyzeImage = AnalyzeImage(objectDetector)

        getDestinationDesc = GetDestinationDesc()

    }

    fun getDatabase(): GraphDatabase {
        return database
    }

    fun getTree(): Tree {
        return tree
    }

    companion object {
        var instance: App? = null
        const val DATABASE_NAME = "nodes"
        const val DATABASE_DIR = "database/nodes.db"
    }
}