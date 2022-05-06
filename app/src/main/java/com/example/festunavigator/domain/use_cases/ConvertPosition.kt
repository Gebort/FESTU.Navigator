package com.example.festunavigator.domain.use_cases

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3

class ConvertPosition {

    operator fun invoke(
        position: Float3,
        translocation: Float3,
        quaternion: Quaternion,
        pivotPosition: Float3
    ): Float3 {
        return (com.google.ar.sceneform.math.Quaternion.rotateVector(quaternion.toOldQuaternion(), (position - pivotPosition).toVector3()).toFloat3() + pivotPosition) - translocation
    }


}