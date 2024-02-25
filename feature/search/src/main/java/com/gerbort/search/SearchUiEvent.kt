package com.gerbort.search

sealed class SearchUiEvent{
    data object SearchSuccess: SearchUiEvent()
    data object SearchInvalid: SearchUiEvent()
}
