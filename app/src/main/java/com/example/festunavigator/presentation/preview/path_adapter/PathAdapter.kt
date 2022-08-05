package com.example.festunavigator.presentation.preview.path_adapter

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.destroy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow


class PathAdapter(
    private val drawerHelper: DrawerHelper,
    private val previewView: ArSceneView,
    private val bufferSize: Int,
    mainScope: LifecycleCoroutineScope
) {

    private val nodes = mutableMapOf<OrientatedPosition, ArNode>()
    private val updateCallback = NodeUpdateCallback()
    private val changesFlow = MutableSharedFlow<DiffOperation>(
        replay = 0,
        extraBufferCapacity = bufferSize,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        mainScope.launchWhenStarted {
            changesFlow.collect { change ->
                when (change) {
                    is DiffOperation.Deleted -> {
                        updateCallback.onRemoved(change.node)
                    }
                    is DiffOperation.Added -> {
                        updateCallback.onInserted(change.node)
                    }
                }
            }
        }
    }

    suspend fun commit(newList: List<OrientatedPosition>) {
        calculateChanges(newList)

    }

    private suspend fun calculateChanges(newList: List<OrientatedPosition>) = withContext(Dispatchers.Default) {
            nodes.keys.asSequence()
                .minus(newList)
                .map { node -> DiffOperation.Deleted(node) }
                .forEach { change ->
                    changesFlow.tryEmit(change)}
            newList.asSequence()
                .minus(nodes.keys)
                .map { node -> DiffOperation.Added(node) }
                .forEach { change -> changesFlow.tryEmit(change) }
    }

    private inner class NodeUpdateCallback {
        suspend fun onInserted(node: OrientatedPosition) {
            if (nodes[node] == null){
                nodes[node] = drawerHelper.placeArrow(node, previewView)

            }
        }

        suspend fun onRemoved(node: OrientatedPosition) {
            nodes[node]?.let {
                nodes.remove(node)
                drawerHelper.removeArrowWithAnim(it)
            }
        }

    }

    sealed class DiffOperation {
        class Added(val node: OrientatedPosition): DiffOperation()
        class Deleted(val node: OrientatedPosition): DiffOperation()
    }

}