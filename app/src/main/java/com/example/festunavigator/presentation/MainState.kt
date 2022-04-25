package com.example.festunavigator.presentation

import com.example.festunavigator.domain.ml.DetectedText
import kotlinx.coroutines.Job

sealed class MainState(
    var previous: MainState? = null
) {

    sealed class ConfirmingState(
        var confirmationObject: ConfirmationObject,
        var result: Boolean? = null
    ): MainState(){

        class InitializeConfirm(
            confirmationObject: ConfirmationObject
        ): ConfirmingState(confirmationObject)

        class EntryConfirm(
            confirmationObject: ConfirmationObject
        ): ConfirmingState(confirmationObject)
    }

    sealed class ScanningState(
        var lastDetectedObject: DetectedText? = null,
        var scanningNow: Boolean = false,
        var currentScanSmoothDelay: Double = 0.0,
        var scanningJob: Job? = null
    ): MainState(){
        class Initialize: ScanningState()
        class EntryCreation: ScanningState()
    }

    object Routing: MainState()
}

