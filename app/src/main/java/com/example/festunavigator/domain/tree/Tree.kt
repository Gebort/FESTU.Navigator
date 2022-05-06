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
            Log.d(TAG, "========")
            Log.d(TAG, "Preload: loaded node, id: ${node.id}, number: ${nodeDto.number}")
            Log.d(TAG, "Pos: ${node.position}")
        }

        for (nodeDto in rawNodesList){
            _allPoints[nodeDto.id]!!.neighbours = nodeDto.neighbours
                .map { id ->
                    allPoints[id]!!
                }
                .toMutableList()
            _links[allPoints[nodeDto.id]!!] = allPoints[nodeDto.id]!!.neighbours
            Log.d(TAG, "Preload: added neighbours for node id: ${nodeDto.id}, size: ${allPoints[nodeDto.id]!!.neighbours.size}")
        }
        Log.d(TAG, "Preload finished")

    }

    suspend fun initialize(entryNumber: String, position: Float3, newRotation: Quaternion): Result<Unit?> {
        Log.d(TAG, "Initialization start")
        if (entryPoints.isNotEmpty()) {
            Log.d(TAG, "Initialization: entry number $entryNumber")
            val entry = entryPoints[entryNumber]
            if (entry != null) {

                pivotPosition= entry.position
                translocation = entry.position - position


                Log.d(TAG, "Initialization: translocation ${translocation}")

                val oldRotation = entry.forwardVector
                //val invertedOldRotation = oldRotation.inverted()
                rotation = convertQuaternion(oldRotation, newRotation.inverted()) * -1f
                rotation.w *= -1f
               // rotation = Quaternion.multiply(newRotation, oldRotation.toOldQuaternion().inverted())

                Log.d(TAG, "Initialization: old rotation ${oldRotation}")
                Log.d(TAG, "Initialization: new rotation ${newRotation}")
                Log.d(TAG, "Initialization: rotation ${rotation}")


                for (node in allPoints.values) {
                    Log.d(TAG, "Initialization: new node =======")

                    //val newPosition = rotation.toNewQuaternion() * (node.position - pivotPosition) + pivotPosition - translocation
                    Log.d(TAG, "Initialization: node ${node.id}")
                    //val y = node.position.y - translocation.y
                    val newPosition = convertPosition(node.position, translocation, rotation, pivotPosition)
                    //val newPosition = node.position - translocation
                   // newPosition.y = y
                    Log.d(TAG, "Initialization: position ${node.position.toVector3()} -> ${newPosition - translocation}")
                    node.position = newPosition

                    if (node is TreeNode.Entry){
                        val newOrientation = convertQuaternion(node.forwardVector, rotation)
                        //val newOrientation = node.forwardVector * rotation.toNewQuaternion()
                        //val newForwardVector = Quaternion.rotateVector(rotation, node.forwardVector)
                        //val newForwardVector = Vector3.up()

                        Log.d(TAG, "Initialization: rotation: ${node.forwardVector} -> ${newOrientation}")
                        node.forwardVector = newOrientation
                    }
                }

                //entry.forwardVector = newRotation.toRotation().toVector3()

                initialized = true
               // Log.d(TAG, "Initialization: rotation vector $rotationVector")
                Log.d(TAG, "Initialization success")

                return Result.success(null)
            } else {
                Log.d(TAG, "Initialization: null entry")
                return Result.failure(
                    exception = WrongEntryException(entryPoints.keys)
                )
            }
        }
        else {
            clearTree()
            initialized = true
            Log.d(TAG, "Initialization: empty entry list, tree cleared")
            Log.d(TAG, "Initialization: rotation vector ${Vector3()}")
            Log.d(TAG, "Initialization success")
            return Result.success(null)
        }
    }

    suspend fun addNode(
        position: Float3,
        number: String? = null,
        forwardVector: Quaternion? = null
    ): TreeNode {
        Log.d(TAG, "Node creation start")
        if (_allPoints.values.find { it.position == position } != null) {
            Log.d(TAG, "Node creation: position taken")
            throw Exception("Position already taken")
        }
        val newNode: TreeNode
        if (number == null){
            Log.d(TAG, "Node creation: null number, returning path")
            newNode = TreeNode.Path(
                size,
                position
            )
        }
        else {
            Log.d(TAG, "Node creation: returning entry")
            if (_entryPoints[number] != null) {
                Log.d(TAG, "Node creation: entry point exists")
                throw Exception("Entry point already exists")
            }
            if (forwardVector == null){
                Log.d(TAG, "Node creation: null forward vector")
                throw Exception("Null forward vector")
            }
            newNode = TreeNode.Entry(
                number,
                forwardVector,
                size,
                position
            )
            _entryPoints[newNode.number] = newNode
            Log.d(TAG, "Node creation: entry added to map, number ${newNode.number}")
            Log.d(TAG, "Node creation: rotation vector ${newNode.forwardVector}")
        }

        _allPoints[newNode.id] = newNode
        Log.d(TAG, "Node creation: node added to map, id ${newNode.id}")
        size++
        Log.d(TAG, "Node creation finish, tree size = $size")
        return newNode
    }

    suspend fun removeNode(
        node: TreeNode
    ) {
        Log.d(TAG, "Remove node start")
        removeAllLinks(node)
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
        if (links[node1] == null) {
            Log.d(TAG, "Link creation: null node1 list")
            _links[node1] = mutableListOf()
        }
        else {
            if (links[node1]!!.contains(node2)) {
                throw Exception("Link already exists")
            }
        }
        if (links[node2] == null) {
            Log.d(TAG, "Link creation: null node2 list")
            _links[node2] = mutableListOf()
        }
        else {
            if (links[node2]!!.contains(node1))
                throw Exception("Link already exists")
        }
        node1.neighbours.add(node2)
        node2.neighbours.add(node1)
        _links[node1] = node1.neighbours
        _links[node2] = node2.neighbours
        Log.d(TAG, "Link ${node1.id} -> ${node2.id}")
        Log.d(TAG, "Link creation finished")
        return true

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
        Log.d(TAG, "Removing all links for node id ${node.id} started")
        if (links[node] == null) {
            Log.d(TAG, "Removing all links: null list")
            Log.d(TAG, "Removing all links: finish")
            return
        }

        for (node2 in node.neighbours) {
            Log.d(TAG, "Removing all links: removed ${node.id} -> ${node2.id}")
            node2.neighbours.remove(node)
            _links[node2] = node2.neighbours
        }

        node.neighbours.clear()
        Log.d(TAG, "Removing all links: node list cleared")
        _links[node] = node.neighbours
        Log.d(TAG, "Removing all links: finish")

    }

    private fun clearTree(){
        Log.d(TAG, "Tree cleared")
        _links.clear()
        _allPoints.clear()
        _entryPoints.clear()
        size = 0
        translocation = Float3(0f, 0f, 0f)
        rotation = Float3(0f, 0f, 0f).toQuaternion()
        pivotPosition = Float3(0f, 0f, 0f)
    }

    companion object {
        const val TAG = "TREE"
    }

}