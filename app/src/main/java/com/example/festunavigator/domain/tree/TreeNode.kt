package com.example.festunavigator.domain.tree

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

sealed class TreeNode(
    val id: Int,
    var position: Float3,
    var neighbours: MutableList<Int> = mutableListOf()
) {

    abstract fun copy(
        id: Int = this.id,
        position: Float3 = this.position,
        neighbours: MutableList<Int> = this.neighbours
    ): TreeNode

    class Entry(
        var number: String,
        var forwardVector: Quaternion,
        id: Int,
        position: Float3,
        neighbours: MutableList<Int> = mutableListOf(),
    ): TreeNode(id, position, neighbours){

        override fun copy(
            id: Int,
            position: Float3,
            neighbours: MutableList<Int>,
        ): Entry {
            return Entry(
                number = this.number,
                forwardVector = this.forwardVector,
                id = id,
                position = position,
                neighbours = neighbours
            )
        }
    }

    class Path(
        id: Int,
        position: Float3,
        neighbours: MutableList<Int> = mutableListOf()
    ): TreeNode(id, position, neighbours) {

        override fun copy(
            id: Int,
            position: Float3,
            neighbours: MutableList<Int>
        ): Path {
            return Path(
                id = id,
                position = position,
                neighbours = neighbours
            )
        }
    }
}