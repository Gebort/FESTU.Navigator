package com.example.festunavigator.data.utils

import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.angleBetweenVectors
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Position
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3
import kotlin.math.sin

fun Vector3.angleBetween(vector3: Vector3): Float {
    return angleBetweenVectors(this, vector3)
}

fun Vector3.rotateBy(quaternion: Quaternion): Vector3 =
    com.google.ar.sceneform.math.Quaternion.rotateVector(quaternion.toOldQuaternion(), this)