package com.gerbort.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gerbort.database.converters.NeighboursConverter
import com.gerbort.database.dao.TreeNodeDao
import com.gerbort.database.dao.RecordsDao
import com.gerbort.database.model.RecordEntity
import com.gerbort.database.model.TreeNodeEntity

@Database(
    entities = [
        TreeNodeEntity::class,
        RecordEntity::class
               ],
    version = 1,
    exportSchema = true,
    autoMigrations = [
    ]
)
@TypeConverters(NeighboursConverter::class)
abstract class AppDatabase: RoomDatabase() {

    abstract val treeNodeDao: TreeNodeDao
    abstract val recordsDao: RecordsDao

}