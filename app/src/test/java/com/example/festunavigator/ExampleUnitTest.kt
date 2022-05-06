package com.example.festunavigator

import com.example.festunavigator.domain.use_cases.ConvertPosition
import com.example.festunavigator.domain.use_cases.ConvertQuaternion
import com.example.festunavigator.domain.use_cases.UndoPositionConvert
import com.example.festunavigator.domain.use_cases.inverted
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.QuaternionComponent
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toQuaternion
import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun undoQuarterionCorrect1() {
        val quart1 = Float3(-0.2f, 0.21f, -0.62f).toQuaternion()
        val quart2 = Float3(0.3f, -0.55f, 0.12f).toQuaternion()
        val quart3 = quart1 * quart2
        val res = (quart1 * quart3.toOldQuaternion().inverted().toNewQuaternion()) * -1f
        res.w *= -1f
        assertEquals(quart2, res)
    }

    @Test
    fun undoQuarterionCorrect2() {
        val convertQuaternion = ConvertQuaternion()
        val oldRotation = Float3(0.2f, 0.33f, -0.51f).toQuaternion()
        val rotation = Float3(0.51f, -0.11f, 0.42f).toQuaternion()
        val newRotation = oldRotation * rotation
        val res = convertQuaternion(oldRotation, newRotation.inverted()) * -1f
        res.w *= -1f
        assertEquals(rotation, res)
    }

    @Test
    fun positionConvert1() {
        val convertPosition = ConvertPosition()
        val pivotPosition = Float3(2f, 2f, 1f)
        val pos = Float3(2f, 4f, 1f)
        val translocation = Float3(-1f, -1f, 0f)
        val rotation = Quaternion.rotationBetweenVectors(
            Vector3(0f, 4f, 1f),
            Vector3(4f, 0f, 1f)
        ).toNewQuaternion()
        val res = convertPosition(pos, translocation, rotation, pivotPosition)
        val actual = Float3(5f, 3f, 1f)
        assertEquals(actual, res)
    }

    @Test
    fun undoPositionConvert1() {
        val undoPosition = UndoPositionConvert()
        val pivotPosition = Float3(2f, 2f, 1f)
        val pos = Float3(5f, 3f, 1f)
        val translocation = Float3(-1f, -1f, 0f)
        val rotation = Quaternion.rotationBetweenVectors(
            Vector3(0f, 4f, 1f),
            Vector3(4f, 0f, 1f)
        ).toNewQuaternion()



        val res = undoPosition(pos, translocation, rotation, pivotPosition)
        val actual = Float3(5f, 3f, 1f)
        assertEquals(actual, res)
    }

}