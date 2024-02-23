package com.gerbort.hit_test

import com.gerbort.common.model.OrientatedPosition
import com.google.ar.core.HitResult

data class HitTestResult(
    val orientatedPosition: OrientatedPosition,
    val hitResult: HitResult
) {
}