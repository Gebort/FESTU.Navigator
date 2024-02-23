package com.gerbort.path_correction.domain

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

interface PathCorrector {

    var onChangeNeeded: (Quaternion) -> Unit

    fun newUserPosition(
        userPos: Float3,
        northPosition: Float3?,
    )

}