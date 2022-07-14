package com.example.festunavigator.domain.tree

import android.util.Log
import com.example.festunavigator.data.model.TreeNodeDto
import com.example.festunavigator.domain.repository.GraphRepository
import com.example.festunavigator.domain.use_cases.convert
import com.example.festunavigator.domain.use_cases.inverted
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Tree(
    private val repository: GraphRepository
) {

    private val _entryPoints: MutableMap<String, TreeNode.Entry> = mutableMapOf()
    private val _allPoints: MutableMap<Int, TreeNode> = mutableMapOf()
    private val _links: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    private val _translocatedPoints: MutableMap<TreeNode, Boolean> = mutableMapOf()

    private var availableId = 0

    var initialized = false
        private set
    var preloaded = false
        private set

    var translocation = Float3(0f, 0f, 0f)
        private set

    var pivotPosition = Float3(0f, 0f, 0f)
        private set

    var rotation = Float3(0f, 0f, 0f).toQuaternion()
        private set

    suspend fun preload() = withContext(Dispatchers.IO) {
        if (initialized){
            throw Exception("Already initialized, cant preload")
        }
        preloaded = false
        val rawNodesList = repository.getNodes()
        for (nodeDto in rawNodesList) {
            var node: TreeNode
            if (nodeDto.type == TreeNodeDto.TYPE_ENTRY
                && nodeDto.number != null
                && nodeDto.forwardVector != null){
                node = TreeNode.Entry(
                    number = nodeDto.number,
                    forwardVector = nodeDto.forwardVector,
                    id = nodeDto.id,
                    position = Float3(nodeDto.x, nodeDto.y, nodeDto.z),
                    neighbours = nodeDto.neighbours
                )
                _entryPoints[node.number] = node
            }
            else {
                node = TreeNode.Path(
                    id = nodeDto.id,
                    position = Float3(nodeDto.x, nodeDto.y, nodeDto.z),
                    neighbours = nodeDto.neighbours
                )
            }
            _allPoints[node.id] = node
            _links[node.id] = node.neighbours
            if (node.id+1 > availableId){
                availableId = node.id+1
            }
        }
        preloaded = true
    }

    suspend fun initialize(entryNumber: String, position: Float3, newRotation: Quaternion): Result<Unit?> {
        initialized = false
        if (_entryPoints.isNotEmpty()) {
            val entry = _entryPoints[entryNumber]
            if (entry != null) {

                pivotPosition = entry.position
                translocation = entry.position - position
                rotation = entry.forwardVector.convert(newRotation.inverted()) * -1f
                rotation.w *= -1f


//                for (node in allPoints.values) {
//                    translocateNode(node)
//
//                }


            initialized = true
            return Result.success(null)

            } else {
                return Result.failure(
                    exception = WrongEntryException(_entryPoints.keys)
                )
            }
        }
        else {
            clearTree()
            initialized = true
            return Result.success(null)
        }
    }

    fun getNode(id: Int): TreeNode? {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        val node = _allPoints[id]
        return if (_translocatedPoints.containsKey(node)){
            node
        } else {
            if (node == null) {
                null
            } else {
                translocateNode(node)
                node
            }
        }
    }

    fun getEntry(number: String): TreeNode.Entry? {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        val entry = _entryPoints[number]
        return if (entry != null) {
            getNode(entry.id) as TreeNode.Entry
        } else {
            null
        }
    }

    fun getNodes(nodes: List<Int>): List<TreeNode> {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        return nodes.mapNotNull {
            getNode(it)
        }
    }

    fun getAllNodes(): List<TreeNode> {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        val nodes = mutableListOf<TreeNode>()
        for (key in _allPoints.keys) {
            getNode(key)?.let {
                nodes.add(it)
            }
        }
        return nodes
    }

    fun getEntriesNumbers(): Set<String> {
        return _entryPoints.keys
    }

//    fun getAllLinksKeys(): Set<Int> {
//        return _links.keys
//    }

    fun getNodesWithLinks(): List<TreeNode> {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
       return _links.keys.mapNotNull {
            getNode(it)
        }
    }

    fun getNodeLinks(node: TreeNode): List<TreeNode>? {
        return _links[node.id]?.mapNotNull { getNode(it) }

    }

    fun hasEntry(number: String): Boolean {
        return _entryPoints.keys.contains(number)
    }

    private fun translocateNode(node: TreeNode) {
        node.position = convertPosition(node.position, translocation, rotation, pivotPosition)
        if (node is TreeNode.Entry){
            node.forwardVector = node.forwardVector.convert(rotation)
        }
        _translocatedPoints[node] = true
    }

    suspend fun addNode(
        position: Float3,
        number: String? = null,
        forwardVector: Quaternion? = null
    ): TreeNode {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        if (_allPoints.values.find { it.position == position } != null) {
            throw Exception("Position already taken")
        }
        val newNode: TreeNode
        if (number == null){
            newNode = TreeNode.Path(
                availableId,
                position
            )
        }
        else {
            if (_entryPoints[number] != null) {
                throw Exception("Entry point already exists")
            }
            if (forwardVector == null){
                throw Exception("Null forward vector")
            }
            newNode = TreeNode.Entry(
                number,
                forwardVector,
                availableId,
                position
            )
            _entryPoints[newNode.number] = newNode
            _translocatedPoints[newNode] = true
        }

        _allPoints[newNode.id] = newNode
        availableId++
        repository.insertNodes(listOf(newNode), translocation, rotation, pivotPosition)
        return newNode
    }

    suspend fun removeNode(
        node: TreeNode
    ) {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        val nodesForUpdate = getNodes(node.neighbours.toMutableList())
        removeAllLinks(node)
        _translocatedPoints.remove(node)
        when (node){
            is TreeNode.Path -> {
                _allPoints.remove(node.id)
            }
            is TreeNode.Entry -> {
                _entryPoints.remove(node.number)
                _allPoints.remove(node.id)
            }
        }
        repository.updateNodes(nodesForUpdate, translocation, rotation, pivotPosition)
        repository.deleteNodes(listOf(node))
    }

    suspend fun addLink(
        node1: TreeNode,
        node2: TreeNode
    ): Boolean {
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        if (_links[node1.id] == null) {
            _links[node1.id] = mutableListOf()
        }
        else {
            if (_links[node1.id]!!.contains(node2.id)) {
                throw Exception("Link already exists")
            }
        }
        if (_links[node2.id] == null) {
            _links[node2.id] = mutableListOf()
        }
        else {
            if (_links[node2.id]!!.contains(node1.id))
                throw Exception("Link already exists")
        }
        node1.neighbours.add(node2.id)
        node2.neighbours.add(node1.id)
        _links[node1.id] = node1.neighbours
        _links[node2.id] = node2.neighbours
        repository.updateNodes(listOf(node1, node2), translocation, rotation, pivotPosition)
        return true

    }

//    suspend fun removeLink(
//        node1: TreeNode,
//        node2: TreeNode
//    ) {
//        //нет обновления в repository!!!!
//        if (!initialized){
//            throw Exception("Tree isnt initialized")
//        }
//        if (_links[node1.id] == null || _links[node2.id] == null)
//            return
//        if (_links[node1.id]!!.contains(node2.id) && _links[node2.id]!!.contains(node1.id)){
//            node1.neighbours.remove(node2.id)
//            node2.neighbours.remove(node1.id)
//            _links[node1.id] = node1.neighbours
//            _links[node2.id] = node2.neighbours
//        }
//    }

    private fun removeAllLinks(node: TreeNode){
        if (!initialized){
            throw Exception("Tree isnt initialized")
        }
        if (_links[node.id] == null) {
            return
        }

        node.neighbours.forEach { id ->
            _allPoints[id]?.let {
                it.neighbours.remove(node.id)
                _links[it.id] = it.neighbours
            }
        }

        node.neighbours.clear()
        _links[node.id] = node.neighbours
    }

    private suspend fun clearTree(){
        Log.d(TAG, "Tree cleared")
        _links.clear()
        _allPoints.clear()
        _entryPoints.clear()
        _translocatedPoints.clear()
        availableId = 0
        translocation = Float3(0f, 0f, 0f)
        rotation = Float3(0f, 0f, 0f).toQuaternion()
        pivotPosition = Float3(0f, 0f, 0f)
        repository.clearNodes()
    }

    private fun convertPosition(
        position: Float3,
        translocation: Float3,
        quaternion: Quaternion,
        pivotPosition: Float3
    ): Float3 {
        return (com.google.ar.sceneform.math.Quaternion.rotateVector(
            quaternion.toOldQuaternion(),
            (position - pivotPosition).toVector3()
        ).toFloat3() + pivotPosition) - translocation
    }

    companion object {
        const val TAG = "TREE"
    }

}