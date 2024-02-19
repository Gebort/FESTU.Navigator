package com.gerbort.data.data.repositories

import com.gerbort.common.model.Record
import com.gerbort.data.data.mappers.toEntity
import com.gerbort.data.data.mappers.toCommon
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.database.dao.RecordsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RecordRepositoryImpl @Inject constructor(
    private val recordsDao: RecordsDao
) : RecordsRepository {
    override suspend fun insertRecord(record: Record) {
        return recordsDao.insertRecord(record.toEntity())
    }

    override fun getRecords(time: Long, limit: Int): Flow<List<Record>> {
        return recordsDao.getRecords(
            time = time,
            limit = limit
        ).map { it.map { it.toCommon() } }
    }

    override fun getRecords(): Flow<List<Record>> {
        return recordsDao.getRecords().map { it.map { it.toCommon() } }
    }
}