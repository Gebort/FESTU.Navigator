package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.pathfinding.Path
import com.example.festunavigator.domain.pathfinding.Pathfinder
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode

class FindWay(
    private val pathfinder: Pathfinder
) {

    suspend operator fun invoke(
        from: String,
        to: String,
        tree: Tree
    ): Path? {
        return pathfinder.findWay(from, to, tree)
    }
}