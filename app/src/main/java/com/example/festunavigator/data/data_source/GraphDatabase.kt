package com.example.festunavigator.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.festunavigator.data.model.NeighboursConverter
import com.example.festunavigator.data.model.TreeNodeDto

@Database(
    entities = [TreeNodeDto::class],
    version = 1
)
@TypeConverters(NeighboursConverter::class)
abstract class GraphDatabase: RoomDatabase() {

    abstract val graphDao: GraphDao

    companion object{
        const val DATABASE_NAME = "graph_db"
    }
}