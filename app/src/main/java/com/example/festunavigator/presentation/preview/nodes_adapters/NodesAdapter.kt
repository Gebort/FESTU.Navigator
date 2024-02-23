package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import com.gerbort.common.model.OrientatedPosition
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
    protected val drawerHelper: DrawerHelper,
    protected val previewView: ArSceneView,
    bufferSize: Int,
    protected val scope: LifecycleCoroutineScope,
) {

    protected val nodes = mutableMapOf<T, ArNode>()
    protected var parentNode: ArNode? = null
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
                            yield()
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
                                yield()
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
            .minus(newList.toSet())
            .map { item -> DiffOperation.Deleted(item) }
            .forEach { change ->
                changesFlow.tryEmit(change)
            }
        newList.asSequence()
            .minus(nodes.keys)
            .map { item -> DiffOperation.Added(item) }
            .forEach { change ->
                changesFlow.tryEmit(change)
            }
    }

    private suspend fun placeParentNode(): ArNode {
        return drawerHelper.placeBlankNode(previewView)
    }

    open fun changeParentPos(newParentPos: Float3? = null, orientation: Quaternion? = null) {
        if (parentNode == null) {
            throw Exception("Parent node is not set")
        }
        newParentPos?.let {
            val diff = it - parentNode!!.position
            if (diff != Float3(0f)) {
                nodes.values.forEach { arNode ->
                    arNode.position -= diff
                }
                parentNode?.position = it
            }
        }
        orientation?.let { q ->
            parentNode?.quaternion = q
        }
    }

    fun getPivot(): OrientatedPosition? {
        parentNode?.let { pn ->
            return OrientatedPosition(
                position = pn.position,
                orientation = pn.quaternion
            )
        }
        return null
    }

    abstract suspend fun onInserted(item: T): ArNode
    abstract suspend fun onRemoved(item: T, node: ArNode)

    sealed class DiffOperation<out T> {
        class Added<out T>(val item: T): DiffOperation<T>()
        class Deleted<out T>(val item: T): DiffOperation<T>()
    }

}