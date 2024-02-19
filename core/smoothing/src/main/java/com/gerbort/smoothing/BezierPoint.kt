package com.gerbort.smoothing

import dev.benedikt.math.bezier.vector.Vector3D

internal data class BezierPoint(
    val t: Double,
    var pos: Vector3D
)