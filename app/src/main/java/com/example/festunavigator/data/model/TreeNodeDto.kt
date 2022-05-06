package com.example.festunavigator.data.model

import androidx.room.*
import com.example.festunavigator.domain.tree.TreeNode
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion

@Entity
class TreeNodeDto(
    @PrimaryKey val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val type: String = TYPE_PATH,
    val number: String? = null,
    val neighbours: MutableList<Int> = mutableListOf(),
    val forwardVector: Quaternion? = null
) {


    companion object {
        val TYPE_PATH = "path"
        val TYPE_ENTRY = "entry"

        fun fromTreeNode(node: TreeNode): TreeNodeDto {
            return TreeNodeDto(
                id = node.id,
                x = node.position.x,
                y = node.position.y,
                z = node.position.z,
                forwardVector = if (node is TreeNode.Entry) node.forwardVector else null,
                type = if (node is TreeNode.Entry) TYPE_ENTRY else TYPE_PATH,
                number = if (node is TreeNode.Entry) node.number else null,
                neighbours = node.neighbours
                    .map { it.id }
                    .toMutableList()
            )
        }

    }
}