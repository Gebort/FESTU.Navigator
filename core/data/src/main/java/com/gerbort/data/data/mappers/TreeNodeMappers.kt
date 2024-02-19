package com.gerbort.data.data.mappers

import com.gerbort.data.domain.model.TreeNode
import com.gerbort.database.model.NodeType
import com.gerbort.database.model.QuaternionWrapper
import com.gerbort.database.model.TreeNodeEntity
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

fun QuaternionWrapper.toQuaternion(): Quaternion {
    return Quaternion(
        x = x,
        y = y,
        z = z,
        w = w
    )
}

fun Quaternion.toQuaternionWrapper(): QuaternionWrapper {
    return QuaternionWrapper(
        x = x,
        y = y,
        z = z,
        w = w
    )
}

fun TreeNodeEntity.toCommon(): TreeNode {
     return if (this.type == NodeType.PATH
        && this.number != null
        && this.forwardVector != null)
         TreeNode.Entry(
             id = id,
             number = number!!,
             position = Float3(x, y, z),
             neighbours = neighbours,
             forwardVector = forwardVector!!.toQuaternion(),
             northDirection = northDirection!!.toQuaternion()
        )

    else
        TreeNode.Path(
            id = id,
            position = Float3(x, y, z),
            neighbours = neighbours,
        northDirection = northDirection!!.toQuaternion()
        )
}

fun TreeNode.toEntity(): TreeNodeEntity {
    return TreeNodeEntity(
        id = id,
        x = position.x,
        y = position.y,
        z = position.z,
        forwardVector = if (this is TreeNode.Entry) forwardVector.toQuaternionWrapper() else null,
        type = if (this is TreeNode.Entry) NodeType.ENTRY else NodeType.PATH,
        number = if (this is TreeNode.Entry) number else null,
        neighbours = neighbours,
        northDirection = northDirection?.toQuaternionWrapper()
    )
}