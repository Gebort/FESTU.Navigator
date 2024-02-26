package com.gerbort.search

import com.gerbort.common.model.Record

data class SearchState(
    val timeRecords: List<Record> = listOf(),
    val entries: List<String> = listOf(),
    val filter: String? = null,
)
