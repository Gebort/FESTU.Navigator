package com.example.festunavigator.presentation

import com.example.festunavigator.domain.ml.DetectedText
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Job

sealed class MainState(
    var previous: MainState? = null,
) {

    sealed class ConfirmingState(
        var labelObject: LabelObject,
        var confirmationObjectJob: Job? = null,
        var confirmationJob: Job? = null,
        var result: Boolean? = null
    ): MainState(){

        class InitializeConfirm(
            labelObject: LabelObject
        ): ConfirmingState(labelObject)

        class EntryConfirm(
            labelObject: LabelObject
        ): ConfirmingState(labelObject)
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
        var startLabel: LabelObject? = null,
        var endLabel: LabelObject? = null
    ): MainState() {
        class Going(
            val wayNodes: MutableList<ArNode> = mutableListOf(),
            var wayBuildingJob: Job? = null,
            startLabel: LabelObject? = null,
            endLabel: LabelObject? = null,
            var startPlacingJob: Job? = null,
            var endPlacingJob: Job? = null

        ): Routing(startLabel, endLabel)
        class Choosing(
            val choosingStart: Boolean,
            var done: Boolean = false,
            startLabel: LabelObject? = null,
            endLabel: LabelObject? = null,
        ): Routing(startLabel, endLabel)
    }

    object Starting: MainState()
}

