package com.example.festunavigator

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.pathfinding.Path
import com.example.festunavigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class PathUnitTest {

    val pos1 = OrientatedPosition(Float3(1f,0f,0f), Quaternion())
    val pos2 = OrientatedPosition(Float3(2f,0f,0f), Quaternion())
    val pos3 = OrientatedPosition(Float3(3f,0f,0f), Quaternion())
    val pos4 = OrientatedPosition(Float3(4f,0f,0f), Quaternion())
    val pos5 = OrientatedPosition(Float3(5f,0f,0f), Quaternion())
    val pos6 = OrientatedPosition(Float3(6f,0f,0f), Quaternion())
    val pos7 = OrientatedPosition(Float3(7f,0f,0f), Quaternion())
    val pos8 = OrientatedPosition(Float3(8f,0f,0f), Quaternion())
    val pos9 = OrientatedPosition(Float3(9f,0f,0f), Quaternion())
    val pos10 = OrientatedPosition(Float3(10f,0f,0f), Quaternion())
    var path = Path(listOf(pos1, pos2, pos3, pos4, pos5, pos6, pos7, pos8, pos9, pos10))

    @Before
    fun setup(){
        path = Path(listOf(pos1, pos2, pos3, pos4, pos5, pos6, pos7, pos8, pos9, pos10))

    }

    @Test
    fun firstSearch1() {
        val nodes = path.getNearNodes(4, Float3(2.1f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
        ), nodes.map { it.position.x })

    }

    @Test
    fun firstSearch2() {
        val nodes = path.getNearNodes(11, Float3(4.59f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
            pos5.position.x,
            pos6.position.x,
            pos7.position.x,
            pos8.position.x,
            pos9.position.x,
            pos10.position.x
        ), nodes.map { it.position.x })
    }

    @Test
    fun firstSearch3() {
        val nodes3 = path.getNearNodes(3, Float3(9.99f, 0f, 0f))
        assertEquals(listOf(
            pos9.position.x,
            pos10.position.x,
        ), nodes3.map { it.position.x })
    }

    @Test
    fun firstSearch4() {
        val nodes = path.getNearNodes(4, Float3(1.1f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
            pos3.position.x,
        ), nodes.map { it.position.x })
    }

    @Test
    fun firstSearch5() {
        val nodes = path.getNearNodes(3, Float3(0.1f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
        ), nodes.map { it.position.x })
    }

    @Test
    fun firstSearch6() {
        val nodes = path.getNearNodes(6, Float3(4.1f, 0f, 0f))
        assertEquals(listOf(
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
            pos5.position.x,
            pos6.position.x,
            pos7.position.x,
            ), nodes.map { it.position.x })
    }

    @Test
    fun firstSearch7() {
        val nodes = path.getNearNodes(7, Float3(4.1f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
            pos5.position.x,
            pos6.position.x,
            pos7.position.x
        ), nodes.map { it.position.x })
    }

    @Test
    fun multipleSearch1() {
        val preset = path.getNearNodes(7, Float3(4.1f, 0f, 0f))
        val nodes = path.getNearNodes(7, Float3(5.1f, 0f, 0f))
        assertEquals(listOf(
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
            pos5.position.x,
            pos6.position.x,
            pos7.position.x,
            pos8.position.x,
            ), nodes.map { it.position.x })
    }

    @Test
    fun multipleSearch2() {
        val preset = path.getNearNodes(7, Float3(4.1f, 0f, 0f))
        val nodes = path.getNearNodes(7, Float3(3.1f, 0f, 0f))
        assertEquals(listOf(
            pos1.position.x,
            pos2.position.x,
            pos3.position.x,
            pos4.position.x,
            pos5.position.x,
            pos6.position.x,
        ), nodes.map { it.position.x })
    }

    @Test
    fun multipleSearch3() {
        val preset = path.getNearNodes(7, Float3(9.1f, 0f, 0f))
        val nodes = path.getNearNodes(7, Float3(8.1f, 0f, 0f))
        assertEquals(listOf(
            pos5.position.x,
            pos6.position.x,
            pos7.position.x,
            pos8.position.x,
            pos9.position.x,
            pos10.position.x,
            ), nodes.map { it.position.x })
    }

    @Test
    fun multipleSearch4() {
        val preset = path.getNearNodes(7, Float3(9.1f, 0f, 0f))
        val preset2 = path.getNearNodes(7, Float3(8.1f, 0f, 0f))
        val preset3 = path.getNearNodes(7, Float3( 6.3f, 0f, 0f))
        val nodes = path.getNearNodes(7, Float3(7.87f, 0f, 0f))
        assertEquals(listOf(
            pos5.position.x,
            pos6.position.x,
            pos7.position.x,
            pos8.position.x,
            pos9.position.x,
            pos10.position.x,
        ), nodes.map { it.position.x })
    }


}