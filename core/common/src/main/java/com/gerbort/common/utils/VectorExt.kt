package com.gerbort.common.utils

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toOldQuaternion

fun Vector3.angleBetween(vector3: Vector3): Float {
    return Vector3.angleBetweenVectors(this, vector3)
}

fun Vector3.rotateBy(quaternion: Quaternion): Vector3 =
    com.google.ar.sceneform.math.Quaternion.rotateVector(quaternion.toOldQuaternion(), this)