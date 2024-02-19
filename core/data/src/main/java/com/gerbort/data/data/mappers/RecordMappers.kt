package com.gerbort.data.data.mappers

import com.gerbort.data.domain.model.Record
import com.gerbort.database.model.RecordEntity

internal fun Record.toEntity(): RecordEntity {
    return RecordEntity(
        id = id,
        start = start,
        end = end,
        time = time
    )
}

internal fun RecordEntity.toCommon(): Record {
    return Record(
        id = id,
        start = start,
        end = end,
        time = time,
    )
}