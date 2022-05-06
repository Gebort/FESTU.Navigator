package com.example.festunavigator.domain.tree

import android.util.Log
import com.example.festunavigator.data.model.TreeNodeDto
import com.example.festunavigator.domain.use_cases.ConvertPosition
import com.example.festunavigator.domain.use_cases.ConvertQuaternion
import com.example.festunavigator.domain.use_cases.inverted
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.*

class Tree(
    private val convertPosition: ConvertPosition = ConvertPosition(),
    private val convertQuaternion: ConvertQuaternion = ConvertQuaternion()
) {

    private val _entryPoints: MutableMap<String, TreeNode.Entry> = mutableMapOf()
    //val entryPoints: Map<String, TreeNode.Entry> = _entryPoints

    private val _allPoints: MutableMap<Int, TreeNode> = mutableMapOf()
    //val allPoints: Map<Int, TreeNode> = _allPoints

    private val _links: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    //val links: Map<TreeNode, List<TreeNode>> = _links

    private val _translocatedPoints: MutableMap<TreeNode, Boolean> = mutableMapOf()

    private var availableId = 0

    var initialized = false
        private set

    var translocation = Float3(0f, 0f, 0f)
        private set

    var pivotPosition = Float3(0f, 0f, 0f)
        private set

    var rotation = Float3(0f, 0f, 0f).toQuaternion()
        private set

    constructor(rawNodesList: List<TreeNodeDto>) : this() {
        Log.d(TAG, "Preload start")

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
            Log.d(TAG, "========")
            Log.d(TAG, "Preload: loaded node, id: ${node.id}, number: ${nodeDto.number}")
            Log.d(TAG, "Pos: ${node.position}")
        }

        Log.d(TAG, "Preload finished")

    }

    suspend fun initialize(entryNumber: String, position: Float3, newRotation: Quaternion): Result<Unit?> {
        Log.d(TAG, "Initialization start")
        if (_entryPoints.isNotEmpty()) {
            Log.d(TAG, "Initialization: entry number $entryNumber")
            val entry = _entryPoints[entryNumber]
            if (entry != null) {

                pivotPosition = entry.position
                translocation = entry.position - position
                rotation = convertQuaternion(entry.forwardVector, newRotation.inverted()) * -1f
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
        return nodes.mapNotNull {
            getNode(it)
        }
    }

    fun getAllNodes(): List<TreeNode> {
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

    fun getAllLinksKeys(): Set<Int> {
        return _links.keys
    }

    fun getNodesWithLinks(): List<TreeNode> {
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
            node.forwardVector = convertQuaternion(node.forwardVector, rotation)
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
        return newNode
    }

    suspend fun removeNode(
        node: TreeNode
    ) {
        Log.d(TAG, "Remove node start")
        removeAllLinks(node)
        _translocatedPoints.remove(node)
        when (node){
            is TreeNode.Path -> {
                Log.d(TAG, "Remove node: node id ${node.id}")
                _allPoints.remove(node.id)
            }
            is TreeNode.Entry -> {
                Log.d(TAG, "Remove node: entry node id ${node.id} number ${node.number}")
                _entryPoints.remove(node.number)
                _allPoints.remove(node.id)
            }
        }
        Log.d(TAG, "Remove node finish")
    }

    suspend fun addLink(
        node1: TreeNode,
        node2: TreeNode
    ): Boolean {
        Log.d(TAG, "Link creation started")
        if (_links[node1.id] == null) {
            Log.d(TAG, "Link creation: null node1 list")
            _links[node1.id] = mutableListOf()
        }
        else {
            if (_links[node1.id]!!.contains(node2.id)) {
                throw Exception("Link already exists")
            }
        }
        if (_links[node2.id] == null) {
            Log.d(TAG, "Link creation: null node2 list")
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
        Log.d(TAG, "Link ${node1.id} -> ${node2.id}")
        Log.d(TAG, "Link creation finished")
        return true

    }

    suspend fun removeLink(
        node1: TreeNode,
        node2: TreeNode
    ) {
        if (_links[node1.id] == null || _links[node2.id] == null)
            return
        if (_links[node1.id]!!.contains(node2.id) && _links[node2.id]!!.contains(node1.id)){
            node1.neighbours.remove(node2.id)
            node2.neighbours.remove(node1.id)
            _links[node1.id] = node1.neighbours
            _links[node2.id] = node2.neighbours
        }
    }

    private fun removeAllLinks(node: TreeNode){
        Log.d(TAG, "Removing all links for node id ${node.id} started")
        if (_links[node.id] == null) {
            Log.d(TAG, "Removing all links: null list")
            Log.d(TAG, "Removing all links: finish")
            return
        }

        node.neighbours.forEach { id ->
            _allPoints[id]?.let {
                it.neighbours.remove(node.id)
                _links[it.id] = it.neighbours
            }
        }

        node.neighbours.clear()
        Log.d(TAG, "Removing all links: node list cleared")
        _links[node.id] = node.neighbours
        Log.d(TAG, "Removing all links: finish")

    }

    private fun clearTree(){
        Log.d(TAG, "Tree cleared")
        _links.clear()
        _allPoints.clear()
        _entryPoints.clear()
        _translocatedPoints.clear()
        availableId = 0
        translocation = Float3(0f, 0f, 0f)
        rotation = Float3(0f, 0f, 0f).toQuaternion()
        pivotPosition = Float3(0f, 0f, 0f)
    }

    companion object {
        const val TAG = "TREE"
    }

}