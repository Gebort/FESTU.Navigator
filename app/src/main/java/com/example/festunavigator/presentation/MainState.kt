package com.example.festunavigator.presentation

import com.example.festunavigator.domain.ml.DetectedText
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Job

sealed class MainState(
    var previous: MainState? = null,
) {

    sealed class ConfirmingState(
        var confirmationObject: ConfirmationObject,
        var confirmationObjectJob: Job? = null,
        var confirmationJob: Job? = null,
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

    sealed class Routing(
        var startNumber: String? = null,
        var endNumber: String? = null
    ): MainState() {
        class Going(
            val wayNodes: MutableList<ArNode>? = null,
            val wayBuildingJob: Job? = null,
            startNumber: String? = null,
            endNumber: String? = null
        ): Routing(startNumber, endNumber)
        class Choosing(
            val choosingStart: Boolean,
            var done: Boolean = false,
            startNumber: String? = null,
            endNumber: String? = null,
        ): Routing(startNumber, endNumber)
    }

    object Starting: MainState()
}

