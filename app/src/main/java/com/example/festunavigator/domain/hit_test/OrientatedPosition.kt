package com.example.festunavigator.domain.hit_test

import com.google.ar.core.HitResult
import dev.romainguy.kotlin.math.Float3

data class OrientatedPosition(
    val position: Float3,
    val orientation: dev.romainguy.kotlin.math.Quaternion,
    val hitResult: HitResult
)