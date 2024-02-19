package com.example.festunavigator.data.repository

import com.example.festunavigator.data.model.Record
import com.example.festunavigator.domain.repository.RecordsRepository
import com.gerbort.database.dao.RecordsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecordsImpl @Inject constructor(
    private val dao: RecordsDao
): RecordsRepository {

    override suspend fun insertRecord(record: Record) {
        dao.insertRecord(record)
    }

    override fun getRecords(time: Long, limit: Int): Flow<List<Record>> {
        return dao.getRecords(time, limit)
    }

    override fun getRecords(): Flow<List<Record>> {
        return dao.getRecords()
    }
}