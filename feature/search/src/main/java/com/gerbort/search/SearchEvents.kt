package com.gerbort.search

sealed interface SearchEvents {

    class TrySearch(val number: String, val searchType: SearchType): SearchEvents

    data object LoadRecords: SearchEvents

}

enum class SearchType(val value: Int) {
    START(0),
    END(1);

    companion object {
        private val map = entries.toTypedArray().associateBy(SearchType::value)
        fun fromInt(type: Int) = map[type] ?: throw IllegalArgumentException("Unknown search type")
    }
}