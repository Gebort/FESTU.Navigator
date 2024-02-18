package com.gerbort.database.dao

import androidx.room.*
import com.gerbort.database.model.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordsDao {

    @Query("SELECT * FROM RecordEntity")
    fun getRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM RecordEntity " +
            "WHERE time < :time  " +
            "GROUP BY `end`" +
            "ORDER BY time DESC " +
            "LIMIT :limit")
    fun getRecords(time: Long, limit: Int = 5): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: RecordEntity)

}