package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.data.utils.multiply
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

abstract class NodesAdapter<T>(
    val drawerHelper: DrawerHelper,
    val previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
) {

    protected val nodes = mutableMapOf<T, ArNode>()
    var parentNode: ArNode? = null
        private set
    private val changesFlow = MutableSharedFlow<DiffOperation<T>>(
        replay = 0,
        extraBufferCapacity = bufferSize,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launchWhenStarted {
            parentNode = placeParentNode()
            changesFlow.collect { change ->
                when (change) {
                    is DiffOperation.Deleted -> {
                        nodes[change.item]?.let {
                            nodes.remove(change.item)
                            parentNode?.removeChild(it)
                            onRemoved(change.item, it)
                        }
                    }
                    is DiffOperation.Added -> {
                        if (nodes[change.item] == null){
                            onInserted(change.item).let {
                                nodes[change.item] = it
                                parentNode?.let { pn ->
                                    it.position -= pn.position
                                    pn.addChild(it)
                                }
                            }
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
                changesFlow.tryEmit(change)
                yield()
            }
        newList.asSequence()
            .minus(nodes.keys)
            .map { item -> DiffOperation.Added(item) }
            .forEach { change ->
                changesFlow.tryEmit(change)
                yield()
            }
    }

    private suspend fun placeParentNode(): ArNode {
        return drawerHelper.placeBlankNode(previewView)
    }

    open fun changeParentPos(newParentPos: Float3? = null, transition: Quaternion? = null) {
        if (parentNode == null) {
            throw Exception("Parent node is not set")
        }
        newParentPos?.let {
            val diff = it - parentNode!!.position
            nodes.values.forEach { node ->
                node.position -= diff
            }
            parentNode?.position = it
        }
        transition?.let { q2 ->
            parentNode?.quaternion?.let { q1 ->
                parentNode?.quaternion = q1.multiply(q2)
            }
        }
    }

    abstract suspend fun onInserted(item: T): ArNode
    abstract suspend fun onRemoved(item: T, node: ArNode)

    sealed class DiffOperation<out T> {
        class Added<out T>(val item: T): DiffOperation<T>()
        class Deleted<out T>(val item: T): DiffOperation<T>()
    }

}