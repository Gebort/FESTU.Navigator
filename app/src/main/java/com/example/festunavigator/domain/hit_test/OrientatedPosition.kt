package com.example.festunavigator.domain.hit_test

import dev.romainguy.kotlin.math.Float3

data class OrientatedPosition(
    val position: Float3,
    val orientation: dev.romainguy.kotlin.math.Quaternion
)