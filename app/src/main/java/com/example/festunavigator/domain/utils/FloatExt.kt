package com.example.festunavigator.domain.utils

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.pow
import kotlin.math.abs
import kotlin.math.sqrt

fun Float3.getApproxDif(pos: Float3): Float {
    return sqrt(+abs(pow(x - pos.x, 2f))
     //       + abs(pow(y - pos.y, 2f))
            + abs(pow(z - pos.z, 2f))
    )
}