package com.gerbort.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gerbort.node_graph.domain.use_cases.CreateNodeUseCase
import com.gerbort.node_graph.domain.use_cases.InitializeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    private val initializeUseCase: InitializeUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
): ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state = _state.asStateFlow()

    private val _confirmUiEvents = Channel<ScannerUiEvents>()
    val confirmUiEvents = _confirmUiEvents.receiveAsFlow()

    fun onEvent(event: ScannerEvent) {
        viewModelScope.launch {
            when (event) {
                is ScannerEvent.NewScanType -> _state.update { it.copy(
                    confirmObject = null,
                    confirmType = event.scanType
                ) }
                is ScannerEvent.NewConfirmationObject -> _state.update { it.copy(
                    confirmObject = event.confObject
                ) }
                is ScannerEvent.RejectObject -> _state.update { it.copy(
                    confirmObject = null
                ) }
                is ScannerEvent.AcceptObject -> acceptObject()
            }
        }
    }

    //TODO передать результат в конфермер
    private suspend fun acceptObject() {
        state.value.confirmObject?.let {
            when (_state.value.confirmType) {

                ConfirmType.INITIALIZE -> {

                    if (initializeUseCase(
                            entryNumber = it.label,
                            position = it.pos.position,
                            newOrientation = it.pos.orientation
                    ) ) {
                        _confirmUiEvents.send(ScannerUiEvents.InitSuccess(it.label))
                    }
                    else {
                        _confirmUiEvents.send(ScannerUiEvents.InitFailed)
                    }

                }

                ConfirmType.ENTRY -> {

                    createNodeUseCase(
                        number = it.label,
                        position = it.pos.position,
                        orientation = it.pos.orientation
                    ).fold(
                        onSuccess = {
                            _confirmUiEvents.send(ScannerUiEvents.EntryCreated)
                        },
                        onFailure = {
                            _confirmUiEvents.send(ScannerUiEvents.EntryAlreadyExists)
                        }
                    )

                }
            }
            _state.update { it.copy(confirmObject = null) }
        }
    }

}