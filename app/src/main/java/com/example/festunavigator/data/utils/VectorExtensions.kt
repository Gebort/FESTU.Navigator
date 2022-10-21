package com.example.festunavigator.data.utils

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.angleBetweenVectors

fun Vector3.angleBetween(vector3: Vector3): Float {
    return angleBetweenVectors(this, vector3)
}