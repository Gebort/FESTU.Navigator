package com.example.festunavigator.domain.tree

import com.example.festunavigator.data.model.TreeNodeDto
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.Position

class Tree(

) {

    private val _entryPoints: MutableMap<String, TreeNode.Entry> = mutableMapOf()
    val entryPoints: Map<String, TreeNode.Entry> = _entryPoints

    private val _allPoints: MutableMap<Int, TreeNode> = mutableMapOf()
    val allPoints: Map<Int, TreeNode> = _allPoints

    private val _links: MutableMap<TreeNode, MutableList<TreeNode>> = mutableMapOf()
    val links: Map<TreeNode, List<TreeNode>> = _links

    var size = 0
        private set

    var initialized = false
        private set

    var translocation = Float3(0f, 0f, 0f)
        private set

    constructor(rawNodesList: List<TreeNodeDto>) : this() {
        for (nodeDto in rawNodesList) {
            var node: TreeNode
            if (nodeDto.type == TreeNodeDto.TYPE_ENTRY && nodeDto.number != null){
                node = TreeNode.Entry(
                    number = nodeDto.number,
                    id = nodeDto.id,
                    position = Float3(nodeDto.x, nodeDto.y, nodeDto.z)
                )
                _entryPoints[node.number] = node
            }
            else {
                node = TreeNode.Path(
                    id = nodeDto.id,
                    position = Float3(nodeDto.x, nodeDto.y, nodeDto.z)
                )
            }
            _allPoints[node.id] = node
            size++
        }

        for (nodeDto in rawNodesList){
            allPoints[nodeDto.id]!!.neighbours = nodeDto.neighbours
                .mapNotNull { id ->
                allPoints[id]
            }
                .toMutableList()
            _links[allPoints[nodeDto.id]!!] = allPoints[nodeDto.id]!!.neighbours
        }
    }

    suspend fun initialize(entryNumber: String, position: Float3): Result<Unit> {

        if (entryPoints.isNotEmpty()) {

            val entry = entryPoints[entryNumber]
            if (entry != null) {
                translocation = Float3(
                    entry.position.x - position.x,
                    entry.position.y - position.y,
                    entry.position.z - position.z,
                )

                for (node in allPoints.values) {
                    node.position.apply {
                        x -= translocation.x
                        y -= translocation.y
                        z -= translocation.z
                    }
                }

                initialized = true

                return Result.success(Unit)
            } else {
                return Result.failure(
                    exception = WrongEntryException(entryPoints.keys)
                )
            }
        }
        else {
            clearTree()
            initialized = true
            return Result.success(Unit)
        }
    }

    suspend fun addNode(
        position: Float3,
        number: String? = null
    ): TreeNode {
        val newNode: TreeNode
        if (number == null){
            newNode = TreeNode.Path(
                size,
                position
            )
        }
        else {
            newNode = TreeNode.Entry(
                number,
                size,
                position
            )
            _entryPoints[newNode.number] = newNode
        }

        _allPoints[newNode.id] = newNode
        size++
        return newNode
    }

    suspend fun removeNode(
        node: TreeNode
    ) {
        removeAllLinks(node)
        when (node){
            is TreeNode.Path -> {
                _allPoints.remove(node.id)
            }
            is TreeNode.Entry -> {
                _entryPoints.remove(node.number)
                _allPoints.remove(node.id)
            }
        }
    }

    suspend fun addLink(
        node1: TreeNode,
        node2: TreeNode
    ) {
        if (links[node1] == null)
            _links[node1] = mutableListOf()
        if (links[node2] == null)
            _links[node2] = mutableListOf()
        node1.neighbours.add(node2)
        node2.neighbours.add(node1)
        _links[node1] = node1.neighbours
        _links[node2] = node2.neighbours

    }

    suspend fun removeLink(
        node1: TreeNode,
        node2: TreeNode
    ) {
        if (links[node1] == null || links[node2] == null)
            return
        if (links[node1]!!.contains(node2) && links[node2]!!.contains(node1)){
            node1.neighbours.remove(node2)
            node2.neighbours.remove(node1)
            _links[node1] = node1.neighbours
            _links[node2] = node2.neighbours
        }
    }

    private fun removeAllLinks(node: TreeNode){
        if (links[node] == null)
            return

        for (node2 in node.neighbours) {
            node2.neighbours.remove(node)
            _links[node2] = node2.neighbours
        }

        node.neighbours.clear()
        _links[node] = node.neighbours

    }

    private fun clearTree(){
        _links.clear()
        _allPoints.clear()
        _entryPoints.clear()
        size = 0
        translocation = Float3(0f, 0f, 0f)
    }

}