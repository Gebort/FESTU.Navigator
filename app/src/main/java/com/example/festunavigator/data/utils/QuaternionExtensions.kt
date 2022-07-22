package com.example.festunavigator.domain.use_cases

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion

fun Quaternion.convert(quaternion2: Quaternion): Quaternion {
    return this * quaternion2
}

fun Quaternion.inverted(): Quaternion {
    return toOldQuaternion().inverted().toNewQuaternion()
}

fun Quaternion.opposite(): Quaternion {
    return inverse(this)
}

fun Quaternion.Companion.lookRotation(forward: Vector3, up: Vector3 = Vector3.up()): Quaternion {
    val rotationFromAtoB = com.google.ar.sceneform.math.Quaternion.lookRotation(forward, up)
    return com.google.ar.sceneform.math.Quaternion.multiply(
        rotationFromAtoB,
        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
    ).toNewQuaternion()
}