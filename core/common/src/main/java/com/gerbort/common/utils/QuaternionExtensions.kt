package com.gerbort.common.utils

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3

fun Quaternion.multiply(quaternion2: Quaternion): Quaternion {
    return this * quaternion2
}

fun Quaternion.transition(endQuaternion: Quaternion): Quaternion {
    return (this.multiply(endQuaternion.inverted()) * -1f).apply { this.w *= -1f }
}

fun Quaternion.inverted(): Quaternion {
    return toOldQuaternion().inverted().toNewQuaternion()
}

fun Quaternion.opposite(): Quaternion {
    return inverse(this)
}

fun Quaternion.convertPosition(
    position: Float3,
    pivotPosition: Float3,
    translocation: Float3 = Float3(0f)
): Float3 {
    return (com.google.ar.sceneform.math.Quaternion.rotateVector(
        this.toOldQuaternion(),
        (position - pivotPosition).toVector3()
    ).toFloat3() + pivotPosition) - translocation
}

fun Quaternion.reverseConvertPosition(
    position: Float3,
    pivotPosition: Float3,
    translocation: Float3 = Float3(0f)
): Float3 = this.inverted().convertPosition(position, pivotPosition, translocation)

fun Quaternion.undoConvertPosition(
    position: Float3,
    pivotPosition: Float3,
    translocation: Float3 = Float3(0f)
): Float3 {
    return rotateVector(
        (position - pivotPosition - translocation).toVector3()
    ).toFloat3() + pivotPosition
}

fun Quaternion.rotateVector(vector3: Vector3): Vector3 = com.google.ar.sceneform.math.Quaternion.rotateVector(
    this.toOldQuaternion(),
    vector3
)

fun Quaternion.Companion.axisAngle(vector: Vector3, degrees: Float): Quaternion {
    return com.google.ar.sceneform.math.Quaternion.axisAngle(vector, degrees).toNewQuaternion()
}

fun Quaternion.Companion.lookRotation(forward: Vector3, up: Vector3 = Vector3.up()): Quaternion {
    val rotationFromAtoB = com.google.ar.sceneform.math.Quaternion.lookRotation(forward, up)
    return com.google.ar.sceneform.math.Quaternion.multiply(
        rotationFromAtoB,
        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
    ).toNewQuaternion()
}

fun Quaternion.Companion.fromVector(vector: Vector3): Quaternion {
    return com.google.ar.sceneform.math.Quaternion.lookRotation(
        vector,
        Vector3.up()
    ).toNewQuaternion()
}