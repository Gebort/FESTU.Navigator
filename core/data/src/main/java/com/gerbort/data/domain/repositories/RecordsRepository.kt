package com.gerbort.data.domain.repositories

import com.gerbort.data.domain.model.Record
import kotlinx.coroutines.flow.Flow

interface RecordsRepository {

    suspend fun insertRecord(record: Record)

    fun getRecords(time: Long, limit: Int): Flow<List<Record>>

    fun getRecords(): Flow<List<Record>>

}