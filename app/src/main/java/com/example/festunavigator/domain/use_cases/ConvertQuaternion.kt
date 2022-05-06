package com.example.festunavigator.domain.use_cases

import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion

class ConvertQuaternion {

    operator fun invoke(
        quaternion1: Quaternion,
        quaternion2: Quaternion
    ): Quaternion {
        return quaternion1 * quaternion2
    }
}

fun Quaternion.inverted(): Quaternion {
    return this.toOldQuaternion().inverted().toNewQuaternion()
}

fun Quaternion.opposite(): Quaternion {
    return inverse(this)
   // return Quaternion.fromEuler(-this.toEulerAngles())
}