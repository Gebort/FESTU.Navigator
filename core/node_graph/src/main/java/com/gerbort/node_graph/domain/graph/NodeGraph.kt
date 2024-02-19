package com.gerbort.node_graph.domain.graph

import com.gerbort.common.model.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

interface NodeGraph {

    fun isPreloaded(): Boolean

    fun isInitialized(): Boolean

    suspend fun preload()

    suspend fun initialize(
        entryNumber: String,
        position: Float3,
        newRotation: Quaternion
    ): Result<Unit>

    fun getNode(id: Int): TreeNode?

    fun getEntry(number: String): TreeNode.Entry?

    fun getEntriesNumbers(): Set<String>

    fun getNodeFromEachRegion(): Map<Int, TreeNode>

    fun hasEntry(number: String): Boolean

    fun hasNode(node: TreeNode): Boolean

    suspend fun addNode(
        position: Float3,
        northDirection: Quaternion?,
        number: String? = null,
        forwardVector: Quaternion? = null
    ): TreeNode

    suspend fun removeNode(node: TreeNode)

    /**
     * Returns true if link was added, or false, if link already exists
     */
    suspend fun addLink(
        node1: TreeNode,
        node2: TreeNode
    ): Boolean

}