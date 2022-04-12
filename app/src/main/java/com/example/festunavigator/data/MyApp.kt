package com.example.festunavigator.data

import android.app.Application
import androidx.room.Room
import com.example.festunavigator.data.data_source.GraphDatabase

class App : Application() {
    private var database: GraphDatabase? = null
    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, GraphDatabase::class.java, "database")
            .allowMainThreadQueries()
            .build()
    }

    fun getDatabase(): GraphDatabase? {
        return database
    }

    companion object {
        var instance: App? = null
    }
}