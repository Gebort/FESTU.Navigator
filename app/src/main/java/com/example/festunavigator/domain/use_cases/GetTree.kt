package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.tree.Tree

class GetTree(
    private val repository: GraphRepository
) {
    suspend operator fun invoke(): Tree {
        return repository.getNodes()
    }
}