package com.example.festunavigator.presentation.preview

import android.annotation.SuppressLint
import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.festunavigator.data.App
import com.example.festunavigator.data.model.Record
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.repository.RecordsRepository
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.tree.WrongEntryException
import com.example.festunavigator.domain.use_cases.FindWay
import com.example.festunavigator.presentation.LabelObject
// NO CONFIRM
//import com.example.festunavigator.presentation.confirmer.ConfirmFragment
import com.example.festunavigator.presentation.preview.state.PathState
import com.example.festunavigator.presentation.search.SearchFragment
import com.example.festunavigator.presentation.search.SearchUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
//MainShareModel is a ViewModel class that handles the shared state between the various components
// in the preview feature of an app. It is responsible for managing the current path state, current
// AR frame, events, and UI states that are used across different parts of the app.

//it takes in three dependencies on construction:a Tree instance, a FindWay instance, and a RecordsRepository instance.
// These dependencies are used by the MainShareModel to perform various operations that affect the shared state of the app.

@HiltViewModel
class MainShareModel @Inject constructor(
    private val tree: Tree,
    private val findWay: FindWay,
    private val records: RecordsRepository
): ViewModel() {

    //The MainShareModel contains several private variables that are exposed to the rest of the app
    // through public StateFlow variables.
    // Each of these variables represents a different aspect of the shared state of the app.

    private var _pathState = MutableStateFlow(PathState())
    val pathState = _pathState.asStateFlow()

    private var _frame = MutableStateFlow<ArFrame?>(null)
    val frame = _frame.asStateFlow()

    private var _mainUiEvents = MutableSharedFlow<MainUiEvent>()
    val mainUiEvents = _mainUiEvents.asSharedFlow()

    private var _searchUiEvents = MutableSharedFlow<SearchUiEvent>()
    val searchUiEvents = _searchUiEvents.asSharedFlow()

    @SuppressLint("StaticFieldLeak")
    private var _confirmationObject = MutableStateFlow<LabelObject?>(null)
    val confirmationObject = _confirmationObject.asStateFlow()

    private var _selectedNode = MutableStateFlow<TreeNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()

    private var _linkPlacementMode = MutableStateFlow(false)
    val linkPlacementMode = _linkPlacementMode.asStateFlow()

    private var _timeRecords = MutableStateFlow<List<Record>>(listOf())
    val timeRecords = _timeRecords.asStateFlow()

    val treeDiffUtils = tree.diffUtils
    val entriesNumber = tree.getEntriesNumbers()

    private var pathfindJob: Job? = null
    private var recordsJob: Job? = null

    init {
        preload()
    }

    //The onEvent() function uses the various dependencies and state variables of the MainShareModel
    //to perform these operations and update the shared state of the app.
    fun onEvent(event: MainEvent) {
        when (event){
            is MainEvent.NewFrame -> {
                //The code is using the viewModelScope.launch function to run the code in a
                // coroutine scope that is tied to the ViewModel.
                // This ensures that the coroutine is cancelled if the ViewModel is destroyed.
                viewModelScope.launch {
                    _frame.emit(event.frame)
                }
            }
            is MainEvent.NewConfirmationObject -> {
                _confirmationObject.update { event.confObject }
            }
            //TrySearch before changes
//            is MainEvent.TrySearch -> {
//                viewModelScope.launch {
//                    processSearch(event.number, event.changeType)
//                }
//            }

            //TrySearch After
            is MainEvent.TrySearch -> {
//
                viewModelScope.launch {
                    event.let {
                        //moved initialization here instead of in confirmFragment
                        initialize(
                            it.number,
                            //@sahar TODO: position and Quaternion should be changed to number's pos & Quat
                            // but since tree isn't initialized yet we set them to center of sceneView for now
                            //Maybe change initialization to RouterFragment and then re-initialize with number's correct pos and Quat
                            Position(0.0f,0.0f,0.0f),
                            Quaternion(0f,0f,0f)
//                            it.pos.position,
//                            it.pos.orientation
                        )
                        processSearch(event.number, event.changeType)
                    }
                }
            }

            //NO CONFIRMFragment
//            is MainEvent.AcceptConfObject -> {
//                when (event.confirmType) {
//                    ConfirmFragment.CONFIRM_INITIALIZE -> {
//                        viewModelScope.launch {
//                            _confirmationObject.value?.let {
//                                initialize(
//                                    it.label,
//                                    it.pos.position,
//                                    it.pos.orientation
//                                )
//                                _confirmationObject.update { null }
//
//                            }
//                        }
//                    }
//                    ConfirmFragment.CONFIRM_ENTRY -> {
//                        viewModelScope.launch {
//                            _confirmationObject.value?.let {
//                                createNode(
//                                    number = it.label,
//                                    position = it.pos.position,
//                                    orientation = it.pos.orientation
//                                )
//                                _confirmationObject.update { null }
//
//                            }
//                        }
//                    }
//                }
//            }
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
        }
    }

    //processSearch : takes two arguments: number of type String and changeType of type Int.
    private suspend fun processSearch(number: String, changeType: Int) {

        //the entry with the given number is retrieved from a tree data structure using the getEntry function
        val entry = tree.getEntry(number)

        //If the entry is not found, a SearchInvalid event is emitted using the _searchUiEvents Flow
        if (entry == null) {
            _searchUiEvents.emit(SearchUiEvent.SearchInvalid)
            return
        }

        // If the entry is found, the PathState is updated based on the value of changeType.
        else {
            //If changeType is SearchFragment.TYPE_START, the startEntry of PathState is updated with the entry
            if (changeType == SearchFragment.TYPE_START) {
                val endEntry = pathState.value.endEntry
                _pathState.update {
                    PathState(
                        startEntry = entry,
                        endEntry = if (entry.number == endEntry?.number) null else endEntry
                    )
                }
            }
            //if changeType is not SearchFragment.TYPE_START, the endEntry of PathState is updated
            // with the entry and the startEntry is updated to null if it has the same number as entry
            else {
                val startEntry = pathState.value.startEntry
                _pathState.update {
                    PathState(
                        startEntry = if (entry.number == startEntry?.number) null else startEntry,
                        endEntry = entry
                    )
                }
            }
            //Поиск окончился удачно // Search completed successfully
            //After the PathState has been updated, the pathfindJob is canceled if it exists and
            // a new job is launched using viewModelScope.launch to call the pathfind function.        pathfindJob?.cancel()
            pathfindJob = viewModelScope.launch {
                pathfind()
            }
            _searchUiEvents.emit(SearchUiEvent.SearchSuccess)

            //saving route to the database

            //Finally, a SearchSuccess event is emitted using the _searchUiEvents Flow and a Record is
            // created and inserted into a database if both startEntry and endEntry of the PathState are not null.
            // The Record contains the number of the startEntry, number of the endEntry, and a timestamp generated using the getCurrentWeekTime function.
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
        //   if (position == null) {
        if (number != null && tree.hasEntry(number)){
            _mainUiEvents.emit(MainUiEvent.EntryAlreadyExists)
            return
        }
        val treeNode = tree.addNode(
            position ?: hitTestResult!!.orientatedPosition.position,
            number,
            orientation
        )

        treeNode.let {
            if (number != null){
                _mainUiEvents.emit(MainUiEvent.EntryCreated)
            }
            _mainUiEvents.emit(MainUiEvent.NodeCreated(
                treeNode,
                hitTestResult?.hitResult?.createAnchor()
            ))
        }
//        } else {
//            val treeNode = tree.addNode(
//                position,
//                number,
//                orientation
//            )
//            treeNode.let {
//                _mainUiEvents.emit(MainUiEvent.NodeCreated(
//                    treeNode,
//                    hitTestResult.hitResult.createAnchor()
//                ))
//            }
//        }
    }

    private suspend fun linkNodes(node1: TreeNode, node2: TreeNode){
        if (tree.addLink(node1, node2)) {
            _linkPlacementMode.update { false }
            _mainUiEvents.emit(MainUiEvent.LinkCreated(node1, node2))
        }
    }

    private suspend fun removeNode(node: TreeNode){
        tree.removeNode(node)
        _mainUiEvents.emit(MainUiEvent.NodeDeleted(node))
        if (node == selectedNode.value) {_selectedNode.update { null }}
        if (node == pathState.value.endEntry) {_pathState.update { it.copy(endEntry = null, path = null) }}
        else if (node == pathState.value.startEntry) {_pathState.update { it.copy(startEntry = null, path = null) }}
    }

    private suspend fun loadRecords() {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            //The getRecords function is called on the records object to fetch the records from the
            // database that have been created within the last 5 days, or 30 minutes in the future
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
        _mainUiEvents.emit(MainUiEvent.InitSuccess)
        val entry = tree.getEntry(entryNumber)
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
