package com.example.festunavigator.domain.repository

sealed class TreeNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val neighbours: MutableList<TreeNode> = mutableListOf()
) {
    class Entry(
        val number: String,
        id: Int,
        x: Float,
        y: Float,
        z: Float,
        neighbours: MutableList<TreeNode> = mutableListOf()
    ): TreeNode(id, x, y, z, neighbours)

    class Path(
        id: Int,
        x: Float,
        y: Float,
        z: Float,
        neighbours: MutableList<TreeNode> = mutableListOf()
    ): TreeNode(id, x, y, z, neighbours)
}