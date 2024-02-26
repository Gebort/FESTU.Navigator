package com.gerbort.data.domain.repositories

import android.icu.util.Calendar
import com.gerbort.common.model.Record
import kotlinx.coroutines.flow.Flow

interface RecordsRepository {

    suspend fun insertRecord(record: Record)

    fun getRecords(time: Long, limit: Int): Flow<List<Record>>

    fun getRecords(): Flow<List<Record>>

}

fun RecordsRepository.getCurrentWeekTime(): Long {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val hour = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    return (dayOfWeek-1)*24*60*60*1000L +
            hour*60*60*1000L +
            minutes*60*1000L
}