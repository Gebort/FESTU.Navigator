package com.example.festunavigator.domain.pathfinding

import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode

interface Pathfinder {

    suspend fun findWay(
        from: String,
        to: String,
        tree: Tree
    ): Path?

}