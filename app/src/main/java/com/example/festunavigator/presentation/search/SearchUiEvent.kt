package com.example.festunavigator.presentation.search

sealed class SearchUiEvent{
    data object SearchSuccess: SearchUiEvent()
    data object SearchInvalid: SearchUiEvent()
}
