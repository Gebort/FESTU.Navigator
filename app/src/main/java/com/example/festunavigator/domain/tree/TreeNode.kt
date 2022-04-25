package com.example.festunavigator.domain.tree

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3

sealed class TreeNode(
    val id: Int,
    var position: Float3,
    var neighbours: MutableList<TreeNode> = mutableListOf()
) {
    class Entry(
        val number: String,
        val forwardVector: Vector3,
        id: Int,
        position: Float3,
        neighbours: MutableList<TreeNode> = mutableListOf(),
    ): TreeNode(id, position, neighbours){

        fun copy(
            number: String = this.number,
            id: Int = this.id,
            position: Float3 = this.position,
            neighbours: MutableList<TreeNode> = this.neighbours,
            forwardVector: Vector3 = this.forwardVector
        ): Entry {
            return Entry(
                number,
                forwardVector,
                id,
                position,
                neighbours,
            )
        }
    }

    class Path(
        id: Int,
        position: Float3,
        neighbours: MutableList<TreeNode> = mutableListOf()
    ): TreeNode(id, position, neighbours) {

        fun copy(
            id: Int = this.id,
            position: Float3 = this.position,
            neighbours: MutableList<TreeNode> = this.neighbours
        ): Path {
            return Path(
                id,
                position,
                neighbours
            )
        }
    }
}