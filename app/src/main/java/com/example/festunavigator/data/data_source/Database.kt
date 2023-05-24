package com.example.festunavigator.data.data_source

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.example.festunavigator.data.model.NeighboursConverter
import com.example.festunavigator.data.model.Record
import com.example.festunavigator.data.model.TreeNodeDto

@Database(
    entities = [TreeNodeDto::class, Record::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
    ]
)
@TypeConverters(NeighboursConverter::class)
abstract class Database: RoomDatabase() {

    abstract val graphDao: GraphDao
    abstract val recordsDao: RecordsDao

}