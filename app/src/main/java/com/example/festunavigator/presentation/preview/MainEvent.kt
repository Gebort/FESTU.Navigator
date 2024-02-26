package com.example.festunavigator.presentation.preview

import dev.romainguy.kotlin.math.Quaternion

sealed interface MainEvent{
    class NewAzimuth(val azimuthRadians: Float): MainEvent
    class PivotTransform(val transition: Quaternion): MainEvent
}
