package com.gerbort.node_graph.domain.use_cases

import com.gerbort.node_graph.domain.graph.NodeGraph
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InitializeUseCase(
    private val nodeGraph: NodeGraph
) {

    suspend operator fun invoke(
        entryNumber: String,
        position: Float3,
        newOrientation: Quaternion
    ): Boolean {
        var result: Result<Unit?>
        withContext(Dispatchers.IO) {
            result = nodeGraph.initialize(entryNumber, position, newOrientation)
        }
        return result.isSuccess
    }

}