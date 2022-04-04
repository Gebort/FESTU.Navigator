package com.example.festunavigator.domain.repository

class Tree {

    private val _entryPoints: MutableMap<String, TreeNode.Entry> = mutableMapOf()
    val entryPoints: Map<String, TreeNode.Entry> = _entryPoints

    private val _allPoints: MutableMap<Int, TreeNode> = mutableMapOf()
    val allPoints: Map<Int, TreeNode> = _allPoints

    private val _links: MutableMap<TreeNode, MutableList<TreeNode>> = mutableMapOf()
    val links: Map<TreeNode, List<TreeNode>> = _links

    var size = allPoints.size
        private set

    fun addNode(
        node: TreeNode
    ) {
        when (node){
            is TreeNode.Path -> {
                _allPoints[node.id] = node
            }
            is TreeNode.Entry -> {
                _entryPoints[node.number] = node
                _allPoints[node.id] = node
            }
        }
    }

    fun removeNode(
        node: TreeNode
    ) {
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

    fun addLink(
        node1: TreeNode,
        node2: TreeNode
    ) {
        if (_links[node1] == null)
            _links[node1] = mutableListOf()
        _links[node1]!!.add(node2)
        node1.neighbours.add(node2)
    }

}