package com.gerbort.node_graph.data.graph

import android.util.Log
import com.gerbort.common.di.AppDispatchers
import com.gerbort.common.di.Dispatcher
import com.gerbort.common.model.OrientatedPosition
import com.gerbort.common.model.TreeNode
import com.gerbort.common.utils.inverted
import com.gerbort.common.utils.multiply
import com.gerbort.common.utils.reverseConvertPosition
import com.gerbort.node_graph.data.diff_utils.GraphDiffUtils
import com.gerbort.node_graph.domain.adapter.NodeRepositoryAdapter
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.graph.NodeGraphDiffUtils
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class NodeGraphImpl @Inject constructor(
    private val nodeAdapter: NodeRepositoryAdapter,
    @Dispatcher(AppDispatchers.Default) private val dispatcher: CoroutineDispatcher
): NodeGraph {

    private val _entryPoints: MutableMap<String, TreeNode.Entry> = mutableMapOf()
    private val _allPoints: MutableMap<Int, TreeNode> = mutableMapOf()
    private val _links: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    private val _regions: MutableMap<Int, Int> = mutableMapOf()

    private val _translocatedPoints: MutableMap<TreeNode, Boolean> = mutableMapOf()

    private var availableId = 0
    private var availableRegion = 0
        get() { return field.also { field++ } }

    private var initialized = MutableStateFlow(false)
    private var preloaded = false

    var translocation = Float3(0f, 0f, 0f)
        private set

    var pivotPosition = Float3(0f, 0f, 0f)
        private set

    var rotation = Float3(0f, 0f, 0f).toQuaternion()
        private set

    val diffUtils: GraphDiffUtils by lazy { GraphDiffUtils(this, dispatcher) }

    private var _treePivot = MutableStateFlow<OrientatedPosition?>(null)

    override fun isPreloaded(): Boolean = preloaded

    override fun isInitialized() = initialized.asStateFlow()

    override suspend fun preload() = withContext(Dispatchers.IO) {
        if (initialized.value){
            throw Exception("Already initialized, cant preload")
        }
        preloaded = false
        val rawNodesList = nodeAdapter.getNodes()
        //TODO заменить на потоковый метод
        for (node in rawNodesList) {
            if (node is TreeNode.Entry ) {
                _entryPoints[node.number] = node
            }
            _allPoints[node.id] = node
            _links[node.id] = node.neighbours
            if (node.id+1 > availableId){
                availableId = node.id+1
            }
        }
        _allPoints.keys.forEach { id -> setRegion(id) }
        preloaded = true
    }

    override suspend fun initialize(
        entryNumber: String,
        position: Float3,
        newRotation: Quaternion
    ): Result<Unit> {
        initialized.update { false }
        if (_entryPoints.isEmpty()) {
            clearTree()
            initialized.update { true }
            return Result.success(Unit)
        }
        else {
            val entry = _entryPoints[entryNumber]
                ?: return Result.failure(
                    exception = GraphException.WrongEntryException(_entryPoints.keys)
                )

            pivotPosition = entry.position
            translocation = entry.position - position
            rotation = entry.forwardVector.multiply(newRotation.inverted()) * -1f
            rotation.w *= -1f

            _treePivot.update { OrientatedPosition(
                position = entry.position,
                orientation = Quaternion()
            ) }

            initialized.update { true }
            return Result.success(Unit)
        }
    }

    override fun getDiffUtils(): NodeGraphDiffUtils {
        return diffUtils
    }

    override fun getTreePivot(): Flow<OrientatedPosition?> = _treePivot.asStateFlow()

    override fun getNode(id: Int): TreeNode? {
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
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

    override fun getEntry(number: String): TreeNode.Entry? {
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
        }
        val entry = _entryPoints[number]
        return if (entry != null) {
            getNode(entry.id) as TreeNode.Entry
        } else {
            null
        }
    }

    override fun getEntriesNumbers(): Set<String> = _entryPoints.keys

    override fun getNodeFromEachRegion(): Map<Int, TreeNode> {
        return _regions.entries
            .distinctBy { it.value}
            .filter { getNode(it.key) != null }
            .associate { it.value to getNode(it.key)!! }
    }

    override fun hasEntry(number: String): Boolean = _entryPoints.keys.contains(number)

    override fun hasNode(node: TreeNode): Boolean = _allPoints.keys.contains(node.id)

    override suspend fun addNode(
        position: Float3,
        northDirection: Quaternion?,
        number: String?,
        forwardDirection: Quaternion?
    ): Result<TreeNode> {
        if (!initialized.value){
            return Result.failure(GraphException.GraphIsntInitialized)
        }
        if (_allPoints.values.find { it.position == position } != null) {
            return Result.failure(AddNodeException.PositionTaken)
        }

        if (number != null && _entryPoints[number] != null){
            return Result.failure(AddNodeException.EntryAlreadyExists)
        }
        //we need to convert position, because if the admin placing new node when other nodes corrected,
        //after reloading new node will be in another place
        val treePivot = _treePivot.value
        val position2 = treePivot?.orientation?.reverseConvertPosition(
            position = position,
            pivotPosition = treePivot.position,
            ) ?: position


        //TODO ПОВОРАЧИВАТЬ СЕВЕР И FORWARD DIRECTION??
        val newNode: TreeNode
        if (number == null){
            newNode = TreeNode.Path(
                id = availableId,
                position = position2,
                northDirection =  northDirection
            )
        }
        else {
            if (forwardDirection == null){
                return Result.failure(AddNodeException.NoForwardDirection)
            }
            newNode = TreeNode.Entry(
                number = number,
                forwardVector = forwardDirection,
                id = availableId,
                position = position2,
                northDirection = northDirection
            )
            _entryPoints[newNode.number] = newNode
        }

        _allPoints[newNode.id] = newNode
        _translocatedPoints[newNode] = true
        setRegion(newNode.id)
        availableId++
        nodeAdapter.insertNodes(listOf(newNode), translocation, rotation, pivotPosition)
        return Result.success(newNode)
    }

    override suspend fun removeNode(node: TreeNode) {
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
        }
        if (!_allPoints.containsKey(node.id)) {
            throw Exception("Unknown node")
        }
        removeAllLinks(node)
        _translocatedPoints.remove(node)
        _allPoints.remove(node.id)
        _regions.remove(node.id)
        if (node is TreeNode.Entry) {
            _entryPoints.remove(node.number)
        }
        nodeAdapter.deleteNodes(listOf(node))
    }

    override suspend fun addLink(node1: TreeNode, node2: TreeNode): Boolean {
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
        }
        if (_links[node1.id] == null) {
            _links[node1.id] = mutableListOf()
        }
        else {
            if (_links[node1.id]!!.contains(node2.id)) {
                return false
            }
        }
        if (_links[node2.id] == null) {
            _links[node2.id] = mutableListOf()
        }
        else {
            if (_links[node2.id]!!.contains(node1.id))
                return false
        }
        node1.neighbours.add(node2.id)
        node2.neighbours.add(node1.id)
        _links[node1.id] = node1.neighbours
        _links[node2.id] = node2.neighbours
        val reg = _regions[node2.id]!!
        setRegion(node1.id, reg, overlap = true, blacklist = listOf(reg))
        nodeAdapter.updateNodes(listOf(node1, node2), translocation, rotation, pivotPosition)
        return true
    }

    private fun getNodes(nodes: List<Int>): List<TreeNode> {
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
        }
        return nodes.mapNotNull {
            getNode(it)
        }
    }

    private suspend fun removeAllLinks(node: TreeNode){
        if (!initialized.value){
            throw GraphException.GraphIsntInitialized
        }
        if (_links[node.id] == null) {
            return
        }

        val nodesForUpdate = getNodes(node.neighbours.toMutableList()).toMutableList()
        nodesForUpdate.add(node)

        node.neighbours.forEach { id ->
            _allPoints[id]?.let {
                it.neighbours.remove(node.id)
                _links[it.id] = it.neighbours
            }
        }
        node.neighbours.clear()
        _links[node.id] = node.neighbours

        //change regions
        val blacklist = mutableListOf<Int>()
        for (i in 0 until nodesForUpdate.size) {
            val id = nodesForUpdate[i].id
            setRegion(id, overlap = true, blacklist = blacklist)
            _regions[id]?.let { blacklist.add(it) }
        }

        nodeAdapter.updateNodes(nodesForUpdate, translocation, rotation, pivotPosition)
    }

    private fun setRegion(nodeId: Int, region: Int? = null, overlap: Boolean = false, blacklist: List<Int> = listOf()) {
        _regions[nodeId]?.let { r ->
            if (blacklist.contains(r) || !overlap) {
                return
            }
        }
        val reg = region ?: availableRegion
        _regions[nodeId] = reg
        //Not using getNode(), because translocation is not needed
        _allPoints[nodeId]?.neighbours?.forEach { id -> setRegion(id, reg)}
    }

    private fun translocateNode(node: TreeNode) {
        node.position = convertPosition(node.position, translocation, rotation, pivotPosition)
        node.northDirection = node.northDirection?.multiply(rotation)
        if (node is TreeNode.Entry){
            node.forwardVector = node.forwardVector.multiply(rotation)
        }
        _translocatedPoints[node] = true
    }

    //TODO заменить на Quarterion.convertPosition
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

    private suspend fun clearTree(){
        Log.d(TAG, "Tree cleared")
        _links.clear()
        _allPoints.clear()
        _entryPoints.clear()
        _translocatedPoints.clear()
        availableId = 0
        availableRegion = 0
        translocation = Float3(0f, 0f, 0f)
        rotation = Float3(0f, 0f, 0f).toQuaternion()
        pivotPosition = Float3(0f, 0f, 0f)
        nodeAdapter.clearNodes()
    }

    private companion object {
        const val TAG = "NodeGraph"
    }

}

sealed class AddNodeException(msg: String): Exception(msg) {
    data object PositionTaken: GraphException("Position already taken")
    data object EntryAlreadyExists: GraphException("Entry already exists")
    data object NoForwardDirection: GraphException("No forward direction")
}

sealed class GraphException(msg: String): Exception(msg) {
    class WrongEntryException(val availableEntries: Set<String>): GraphException("Wrong entry number")
    data object GraphIsntInitialized: GraphException("Tree isnt initialized")
}