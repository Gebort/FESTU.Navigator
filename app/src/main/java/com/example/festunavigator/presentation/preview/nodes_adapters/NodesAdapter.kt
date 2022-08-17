package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

abstract class NodesAdapter<T>(
    val drawerHelper: DrawerHelper,
    val previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope
) {

    protected val nodes = mutableMapOf<T, ArNode>()
    private val changesFlow = MutableSharedFlow<DiffOperation<T>>(
        replay = 0,
        extraBufferCapacity = bufferSize,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launchWhenStarted {
            changesFlow.collect { change ->
                when (change) {
                    is DiffOperation.Deleted -> {
                        nodes[change.item]?.let {
                            nodes.remove(change.item)
                            onRemoved(change.item, it)
                        }
                    }
                    is DiffOperation.Added -> {
                        if (nodes[change.item] == null){
                            nodes[change.item] = onInserted(change.item)
                        }
                    }
                }
            }
        }
    }

    suspend fun commit(newList: List<T>) {
        calculateChanges(newList)
    }

    private suspend fun calculateChanges(newList: List<T>) = withContext(Dispatchers.Default) {
        nodes.keys.asSequence()
            .minus(newList)
            .map { item -> DiffOperation.Deleted(item) }
            .forEach { change ->
                changesFlow.tryEmit(change)}
        newList.asSequence()
            .minus(nodes.keys)
            .map { item -> DiffOperation.Added(item) }
            .forEach { change -> changesFlow.tryEmit(change) }
    }

    abstract suspend fun onInserted(item: T): ArNode
    abstract suspend fun onRemoved(item: T, node: ArNode)

    sealed class DiffOperation<out T> {
        class Added<out T>(val item: T): DiffOperation<T>()
        class Deleted<out T>(val item: T): DiffOperation<T>()
    }

}