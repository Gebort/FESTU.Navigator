package com.example.festunavigator.domain.repository

import dev.romainguy.kotlin.math.Float3

sealed class TreeNode(
    val id: Int,
    val position: Float3,
    val neighbours: MutableList<TreeNode> = mutableListOf()
) {
    class Entry(
        val number: String,
        id: Int,
        position: Float3,
        neighbours: MutableList<TreeNode> = mutableListOf()
    ): TreeNode(id, position, neighbours)

    class Path(
        id: Int,
        position: Float3,
        neighbours: MutableList<TreeNode> = mutableListOf()
    ): TreeNode(id, position, neighbours)
}