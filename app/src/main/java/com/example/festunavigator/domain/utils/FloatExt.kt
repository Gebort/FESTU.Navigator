package com.example.festunavigator.domain.utils

import dev.romainguy.kotlin.math.Float3
import kotlin.math.abs
import kotlin.math.sqrt

fun Float3.getApproxDif(pos: Float3): Float {
    return sqrt(+abs(x - pos.x)
            + abs(y - pos.y)
            + abs(z - z)
    )
}