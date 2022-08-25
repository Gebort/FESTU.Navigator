package com.example.festunavigator.data

import android.app.Application
import androidx.room.Room
import com.example.festunavigator.data.data_source.GraphDatabase
import com.example.festunavigator.data.ml.classification.TextAnalyzer
import com.example.festunavigator.data.pathfinding.AStarImpl
import com.example.festunavigator.data.repository.GraphImpl
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.use_cases.*
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        const val ADMIN_MODE = "ADMIN"
        const val USER_MODE = "USER"
        const val mode = ADMIN_MODE
    }
}