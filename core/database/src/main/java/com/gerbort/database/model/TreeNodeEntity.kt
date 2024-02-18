package com.gerbort.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class TreeNodeEntity(
    @PrimaryKey
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val type: NodeType = NodeType.PATH,
    val number: String? = null,
    val neighbours: MutableList<Int> = mutableListOf(),
    @Embedded(prefix = "north_quaternion_")
    val northDirection: QuaternionWrapper?,
    @Embedded(prefix = "forward_quaternion_")
    val forwardVector: QuaternionWrapper? = null,
)

enum class NodeType {
    PATH,
    ENTRY
}

class QuaternionWrapper(
    @ColumnInfo(name = "x")
    val x: Float,
    @ColumnInfo(name = "y")
    val y: Float,
    @ColumnInfo(name = "z")
    val z: Float,
    @ColumnInfo(name = "w")
    val w: Float
)


