package com.example.festunavigator.domain.smoothing

import dev.benedikt.math.bezier.vector.Vector3D

data class BezierPoint(
    val t: Double,
    var pos: Vector3D
)
