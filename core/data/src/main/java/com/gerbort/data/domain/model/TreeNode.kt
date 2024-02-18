package com.gerbort.data.domain.model

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

sealed class TreeNode(
    val id: Int,
    var position: Float3,
    var neighbours: MutableList<Int> = mutableListOf(),
    var northDirection: Quaternion?
) {

    abstract fun copy(
        id: Int = this.id,
        position: Float3 = this.position,
        neighbours: MutableList<Int> = this.neighbours,
        northDirection: Quaternion? = this.northDirection
    ): TreeNode

    class Entry(
        var number: String,
        var forwardVector: Quaternion,
        northDirection: Quaternion?,
        id: Int,
        position: Float3,
        neighbours: MutableList<Int> = mutableListOf(),
    ): TreeNode(id, position, neighbours, northDirection){

        override fun copy(
            id: Int,
            position: Float3,
            neighbours: MutableList<Int>,
            northDirection: Quaternion?
        ): Entry {
            return Entry(
                number = this.number,
                forwardVector = this.forwardVector,
                id = id,
                northDirection = northDirection,
                neighbours = neighbours,
                position = position
            )
        }
    }

    class Path(
        id: Int,
        position: Float3,
        neighbours: MutableList<Int> = mutableListOf(),
        northDirection: Quaternion?,
    ): TreeNode(id, position, neighbours, northDirection) {

        override fun copy(
            id: Int,
            position: Float3,
            neighbours: MutableList<Int>,
            northDirection: Quaternion?,
        ): Path {
            return Path(
                id = id,
                position = position,
                neighbours = neighbours,
                northDirection = northDirection,
            )
        }
    }
}
