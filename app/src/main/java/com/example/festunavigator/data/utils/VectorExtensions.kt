package com.example.festunavigator.data.utils

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.angleBetweenVectors
import io.github.sceneview.math.Position
import io.github.sceneview.math.toVector3
import kotlin.math.sin

fun Vector3.angleBetween(vector3: Vector3): Float {
    return angleBetweenVectors(this, vector3)
}