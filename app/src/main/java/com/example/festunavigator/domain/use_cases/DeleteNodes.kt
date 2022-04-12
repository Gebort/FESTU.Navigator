package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.TreeNode

class DeleteNodes(
    private val repository: GraphRepository
) {
    suspend operator fun invoke(nodes: List<TreeNode>){
        repository.deleteNodes(nodes)
    }
}