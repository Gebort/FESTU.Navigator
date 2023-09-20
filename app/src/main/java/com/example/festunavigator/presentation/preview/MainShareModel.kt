package com.example.festunavigator.presentation.preview

import android.annotation.SuppressLint
import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.festunavigator.data.App
import com.example.festunavigator.data.model.Record
import com.example.festunavigator.data.utils.*
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.repository.RecordsRepository
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.tree.WrongEntryException
import com.example.festunavigator.domain.use_cases.FindWay
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.confirmer.ConfirmFragment
import com.example.festunavigator.presentation.preview.state.PathState
import com.example.festunavigator.presentation.search.SearchFragment
import com.example.festunavigator.presentation.search.SearchUiEvent
import com.google.ar.sceneform.math.Vector3
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.RotationsOrder
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.quaternion
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainShareModel @Inject constructor(
    private val tree: Tree,
    private val findWay: FindWay,
    private val records: RecordsRepository
): ViewModel() {

    private var _pathState = MutableStateFlow(PathState())
    val pathState = _pathState.asStateFlow()

    private var _frame = MutableStateFlow<ArFrame?>(null)
    val frame = _frame.asStateFlow()

    private var _mainUiEvents = MutableSharedFlow<MainUiEvent>()
    val mainUiEvents = _mainUiEvents.asSharedFlow()

    private var _searchUiEvents = MutableSharedFlow<SearchUiEvent>()
    val searchUiEvents = _searchUiEvents.asSharedFlow()

    var northLocation: Float3? = null
        private set

    @SuppressLint("StaticFieldLeak")
    private var _confirmationObject = MutableStateFlow<LabelObject?>(null)
    val confirmationObject = _confirmationObject.asStateFlow()

    private var _selectedNode = MutableStateFlow<TreeNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()

    private var _linkPlacementMode = MutableStateFlow(false)
    val linkPlacementMode = _linkPlacementMode.asStateFlow()

    private var _timeRecords = MutableStateFlow<List<Record>>(listOf())
    val timeRecords = _timeRecords.asStateFlow()

    private var _treePivot = MutableStateFlow<OrientatedPosition?>(null)
    val treePivot = _treePivot.asStateFlow()

    val treeDiffUtils = tree.diffUtils
    val entriesNumber = tree.getEntriesNumbers()

    private var pathfindJob: Job? = null
    private var recordsJob: Job? = null

    init {
        preload()
    }

    fun onEvent(event: MainEvent) {
        when (event){
            is MainEvent.NewFrame -> {
                viewModelScope.launch {
                    _frame.emit(event.frame)
                }
            }
            is MainEvent.NewAzimuth -> {
                newNorthLocation(event.azimuthRadians)
            }
            is MainEvent.NewConfirmationObject -> {
                _confirmationObject.update { event.confObject }
            }
            is MainEvent.TrySearch -> {
                viewModelScope.launch {
                    processSearch(event.number, event.changeType)
                }
            }
            is MainEvent.AcceptConfObject -> {
                when (event.confirmType) {
                    ConfirmFragment.CONFIRM_INITIALIZE -> {
                        viewModelScope.launch {
                            _confirmationObject.value?.let {
                                initialize(
                                    it.label,
                                    it.pos.position,
                                    it.pos.orientation
                                )
                                _confirmationObject.update { null }

                            }
                        }
                    }
                    ConfirmFragment.CONFIRM_ENTRY -> {
                        viewModelScope.launch {
                            _confirmationObject.value?.let {
                                createNode(
                                    number = it.label,
                                    position = it.pos.position,
                                    orientation = it.pos.orientation
                                )
                                _confirmationObject.update { null }

                            }
                        }
                    }
                }
            }
            is MainEvent.RejectConfObject -> {
                viewModelScope.launch {
                    _confirmationObject.update { null }
                }
            }
            is MainEvent.NewSelectedNode -> {
                viewModelScope.launch {
                    _selectedNode.update { event.node }
                }
            }
            is MainEvent.LoadRecords -> {
                viewModelScope.launch {
                    loadRecords()
                }
            }
            is MainEvent.ChangeLinkMode -> {
                viewModelScope.launch {
                    _linkPlacementMode.update { !linkPlacementMode.value }
                }
            }
            is MainEvent.CreateNode -> {
                viewModelScope.launch {
                    createNode(
                        number = event.number,
                        position = event.position,
                        orientation = event.orientation,
                        hitTestResult = event.hitTestResult
                    )
                }
            }
            is MainEvent.LinkNodes -> {
                viewModelScope.launch {
                    linkNodes(event.node1, event.node2)
                }
            }
            is MainEvent.DeleteNode -> {
                viewModelScope.launch {
                    removeNode(event.node)
                }
            }
            is MainEvent.PivotTransform -> {
                viewModelScope.launch {
                    _treePivot.value?.let { tp ->
                        _treePivot.update { tp.copy(orientation = tp.orientation.multiply(event.transition)) }
                    }
                }
            }
        }
    }

    private suspend fun processSearch(number: String, changeType: Int) {
        val entry = tree.getEntry(number)
        if (entry == null) {
            _searchUiEvents.emit(SearchUiEvent.SearchInvalid)
            return
        } else {

            if (changeType == SearchFragment.TYPE_START) {
                val endEntry = pathState.value.endEntry
                _pathState.update {
                    PathState(
                        startEntry = entry,
                        endEntry = if (entry.number == endEntry?.number) null else endEntry
                 )
                }
            } else {
                val startEntry = pathState.value.startEntry
            _pathState.update {
                PathState(
                    startEntry = if (entry.number == startEntry?.number) null else startEntry,
                    endEntry = entry
                )
            }
        }
        //Search successful
        pathfindJob?.cancel()
        pathfindJob = viewModelScope.launch {
            pathfind()
        }
        _searchUiEvents.emit(SearchUiEvent.SearchSuccess)

         //saving route to the database
        pathState.value.startEntry?.let { start ->
            pathState.value.endEntry?.let { end ->
                val record = Record(
                    start = start.number,
                    end = end.number,
                    time = getCurrentWeekTime()
                )
                records.insertRecord(record)
            }
        }

    }
    }

    private suspend fun pathfind(){
        val from = pathState.value.startEntry?.number ?: return
        val to = pathState.value.endEntry?.number ?: return
        if (tree.getEntry(from) != null && tree.getEntry(to) != null) {
            val path = findWay(from, to, tree)
            if (path != null) {
                _pathState.update { it.copy(
                    path = path
                ) }
            } else {
                _mainUiEvents.emit(MainUiEvent.PathNotFound)
            }
        }
        else {
            throw Exception("Unknown tree nodes")
        }
    }

    private suspend fun createNode(
        number: String? = null,
        position: Float3? = null,
        orientation: Quaternion? = null,
        hitTestResult: HitTestResult? = null,
    ) {
        if (position == null && hitTestResult == null){
            throw Exception("No position was provided")
        }

        if (number != null && tree.hasEntry(number)){
            _mainUiEvents.emit(MainUiEvent.EntryAlreadyExists)
            return
        }
        //we need to convert position, because if the admin placing new node when other nodes corrected,
        //after reloading new node will be in another place
        treePivot.value.let { tp ->
            northLocation.let { north ->
                val position2 = tp?.orientation?.reverseConvertPosition(
                    position = position ?: hitTestResult!!.orientatedPosition.position,
                    pivotPosition = tp.position,
                )
                    ?: position
                    ?: hitTestResult!!.orientatedPosition.position
                val northDirection = when (north) {
                    null -> null
                    else -> Quaternion.fromVector((north - position2).toVector3().normalized())
                }
                val treeNode = tree.addNode(
                    position = position2,
                    number = number,
                    northDirection = northDirection,
                    forwardVector = orientation
                )
                treeNode.let {
                    if (number != null) {
                        _mainUiEvents.emit(MainUiEvent.EntryCreated)
                    }
                    _mainUiEvents.emit(
                        MainUiEvent.NodeCreated(
                            treeNode,
                            hitTestResult?.hitResult?.createAnchor()
                        )
                    )
                }
            }
        }
    }

    private suspend fun linkNodes(node1: TreeNode, node2: TreeNode){
        when (tree.addLink(node1, node2)) {
            is Reaction.Success -> {
                _linkPlacementMode.update { false }
                _mainUiEvents.emit(MainUiEvent.LinkCreated(node1, node2))
            }
            is Reaction.Error -> {
                _mainUiEvents.emit(MainUiEvent.LinkAlreadyExists)
            }
        }
    }

    private suspend fun removeNode(node: TreeNode){
        tree.removeNode(node)
        _mainUiEvents.emit(MainUiEvent.NodeDeleted(node))
        if (node == selectedNode.value) {_selectedNode.update { null }}
        if (node == pathState.value.endEntry) {_pathState.update { it.copy(endEntry = null, path = null) }}
        else if (node == pathState.value.startEntry) {_pathState.update { it.copy(startEntry = null, path = null) }}
    }

    private fun newNorthLocation(azimuth: Float) {
        frame.value?.let {
            val rotation = Quaternion.fromEuler(yaw = azimuth, order = RotationsOrder.ZYX)
            val cameraDirection = it.camera.pose.rotation.copy(z = 0f).toQuaternion()
            val northDirection = cameraDirection * rotation
            this.northLocation = Vector3(Float.MAX_VALUE, 0f, 0f).rotateBy(northDirection).toFloat3()
        }
    }

    private suspend fun loadRecords() {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            val time = getCurrentWeekTime() + 30*60*1000L
            records.getRecords(time, 5).collectLatest{ records ->
                _timeRecords.emit(records)
            }
        }
    }

    private suspend fun initialize(entryNumber: String, position: Float3, newOrientation: Quaternion): Boolean {
        var result: Result<Unit?>
        withContext(Dispatchers.IO) {
            result = tree.initialize(entryNumber, position, newOrientation)
        }
        if (result.isFailure){
            _mainUiEvents.emit(MainUiEvent.InitFailed(
                result.exceptionOrNull() as java.lang.Exception?
            ))
            return false
        }
        val entry = tree.getEntry(entryNumber)
        _mainUiEvents.emit(MainUiEvent.InitSuccess(entry))
        _treePivot.update { OrientatedPosition(
            position = entry?.position ?: Float3(0f),
            orientation = Quaternion()
        ) }
        if (entry != null){
            _pathState.update { PathState(
                startEntry = entry
            ) }
        }
        else {
            _pathState.update { PathState() }
        }
        return true
    }

    private fun preload(){
        viewModelScope.launch {
            tree.preload()
        }
    }

    private fun getCurrentWeekTime(): Long {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        return (dayOfWeek-1)*24*60*60*1000L +
                hour*60*60*1000L +
                minutes*60*1000L
    }
}
