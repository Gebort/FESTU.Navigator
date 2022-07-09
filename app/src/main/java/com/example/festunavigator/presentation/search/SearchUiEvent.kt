package com.example.festunavigator.presentation.search

sealed class SearchUiEvent{
    object SearchSuccess: SearchUiEvent()
    object SearchInvalid: SearchUiEvent()
}
