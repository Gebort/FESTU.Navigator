package com.gerbort.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouterViewModel @Inject constructor(): ViewModel() {

    private val _state = MutableStateFlow(RouterState())
    val state = _state.asStateFlow()

    fun onEvent(event: RouterEvent) {
        viewModelScope.launch {
            when (event) {
                is RouterEvent.ChangeLinkMode -> _state.update { it.copy(linkPlacement = !state.value.linkPlacement) }
                is RouterEvent.CreateNode -> {}
                is RouterEvent.DeleteNode -> {}
                is RouterEvent.LinkNodes -> {}
            }
        }
    }

}