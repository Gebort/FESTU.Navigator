package com.example.festunavigator.domain.tree

import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

sealed class TreeNode(
    val id: Int,
    var position: Float3,
    var neighbours: MutableList<TreeNode> = mutableListOf()
) {
    class Entry(
        var number: String,
        var forwardVector: Quaternion,
        id: Int,
        position: Float3,
        neighbours: MutableList<TreeNode> = mutableListOf(),
    ): TreeNode(id, position, neighbours){

        fun copy(
            number: String = this.number,
            id: Int = this.id,
            position: Float3 = this.position,
            neighbours: MutableList<TreeNode> = this.neighbours,
            forwardVector: Quaternion = this.forwardVector
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