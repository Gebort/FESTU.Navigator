package com.example.festunavigator

import com.example.festunavigator.data.utils.*
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.pathfinding.path_restoring.DirectionTracker
import com.example.festunavigator.domain.utils.meanPoint
import dev.romainguy.kotlin.math.Float3
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toRotation
import io.github.sceneview.math.toVector3
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DirectionTrackerUnitTest {

    val pos1 = Float3(1f,0f,0f)
    val pos2 = Float3(3f,0f,0f)
    val pos3 = Float3(6f,0f,0f)
    val pos4 = Float3(9f,0f,0f)
    val pos5 = Float3(10f,0f,0f)
    val pos6 = Float3(14f,0f,0f)
    val pos7 = Float3(15f,3f,0f)
    val pos8 = Float3(16f,6f,0f)
    val pos9 = Float3(17f,9f,0f)
    val pos10 = Float3(18f,12f,0f)
    var path = listOf(pos1, pos2, pos3, pos4, pos5, pos6, pos7, pos8, pos9, pos10)

    @Test
    fun test1() {
        val tracker = DirectionTracker()
        path.drop(1).take(4).forEach { pos ->
            tracker.newPosition(pos)
        }
        val tpos1 = tracker.getStraightOrientation()
        val tpos2 = OrientatedPosition(
            position = pos1.meanPoint(pos5),
            orientation = Quaternion.fromVector((pos2 - pos1).toVector3())
        )
        assertEquals(tpos2, tpos1)

    }

    @Test
    fun test2() {
        val tracker = DirectionTracker()
        path.drop(1).forEach { pos ->
            tracker.newPosition(pos)
        }
        val tpos1 = tracker.getStraightOrientation()
        val tpos2 = OrientatedPosition(
            position = pos6.meanPoint(pos10),
            orientation = Quaternion.fromVector((pos7 - pos6).toVector3())
        )
        assertEquals(tpos2, tpos1)
    }

    @Test
    fun test3() {
        val v1 = Vector3(1f, -1f, 0f)
        val v2 = Vector3(1f, 0f, 0f)

        val q1 = Quaternion.fromVector(v1) * Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 45f)
        val q2 = Quaternion.fromVector(v2)

        val t = q1.transition(q2)
        val r = t.toRotation()

        assertEquals(r, t)
    }

    @Test
    fun test4() {
        val n = 5
        val list = MutableList(n) {0}
        list.add(1)
        list.add(2)
        list.add(3)
        list.add(4)
        list.add(5)
        list.add(6)
        assertEquals(mutableSetOf(2,3,4,5,6), list)
    }
}