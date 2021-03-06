package com.example.festunavigator.presentation.preview

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.festunavigator.data.App
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.confirmer.ConfirmFragment
import com.example.festunavigator.presentation.preview.state.PathState
import com.example.festunavigator.presentation.search.SearchFragment
import com.example.festunavigator.presentation.search.SearchUiEvent
import com.google.ar.core.Anchor
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.destroy
import io.github.sceneview.node.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainShareModel: ViewModel() {

    private val findWay = App.instance!!.findWay

    private var _pathState = MutableStateFlow(PathState())
    val pathState = _pathState.asStateFlow()

    private var _frame = MutableSharedFlow<ArFrame>()
    val frame = _frame.asSharedFlow()

    private var _mainUiEvents = MutableSharedFlow<MainUiEvent>()
    val mainUiEvents = _mainUiEvents.asSharedFlow()

    private var _searchUiEvents = MutableSharedFlow<SearchUiEvent>()
    val searchUiEvents = _searchUiEvents.asSharedFlow()

    @SuppressLint("StaticFieldLeak")
    private var _confirmationObject = MutableStateFlow<LabelObject?>(null)
    val confirmationObject = _confirmationObject.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    var session: ArSession? = null
        private set

    var tree = App.instance!!.getTree()
        private set

    private var pathfindJob: Job? = null

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
            is MainEvent.NewSession -> {
                session = event.session
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
                        //TODO ???????????????????????????? conf Object ?? ?????????? Entry
                    }
                }
            }
            is MainEvent.RejectConfObject -> {
                viewModelScope.launch {
                    _confirmationObject.update { null }
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

    private suspend fun processSearch(number: String, changeType: Int) {
        if (!tree.hasEntry(number)) {
            _searchUiEvents.emit(SearchUiEvent.SearchInvalid)
            return
        }

        val treeNode = tree.getEntry(number)!!
        val labelObject = LabelObject(
            label = treeNode.number,
            pos = OrientatedPosition(treeNode.position, treeNode.forwardVector),
        )

        if (changeType == SearchFragment.TYPE_START) {
            val endLabel = pathState.value.endLabel
            _pathState.update {
                PathState(
                    startLabel = labelObject,
                    endLabel = if (labelObject.label == endLabel?.label) null else endLabel
                )
            }
        } else {
            val startLabel = pathState.value.startLabel
            _pathState.update {
                PathState(
                    startLabel = if (labelObject.label == startLabel?.label) null else startLabel,
                    endLabel = labelObject
                )
            }
        }
        //?????????? ?????????????????? ????????????
        pathfindJob?.cancel()
        pathfindJob = viewModelScope.launch {
            pathfind()
        }
        _searchUiEvents.emit(SearchUiEvent.SearchSuccess)
    }

    private suspend fun pathfind(){
        val from = pathState.value.startLabel?.label ?: return
        val to = pathState.value.endLabel?.label ?: return
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
        hitTestResult: HitTestResult,
    ) {
     //   if (position == null) {
                val treeNode = tree.addNode(
                    position ?: hitTestResult.orientatedPosition.position,
                    number,
                    orientation
                )
                treeNode.let {
                    _mainUiEvents.emit(MainUiEvent.NodeCreated(
                        treeNode,
                        hitTestResult.hitResult.createAnchor()
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
            _mainUiEvents.emit(MainUiEvent.LinkCreated(node1, node2))
        }
    }

    private suspend fun removeNode(node: TreeNode){
        tree.removeNode(node)
        _mainUiEvents.emit(MainUiEvent.NodeDeleted(node))
    }

    private suspend fun initialize(entryNumber: String, position: Float3, newOrientation: Quaternion): Boolean {
        var result: Result<Unit?>
        withContext(Dispatchers.IO) {
            result = tree.initialize(entryNumber, position, newOrientation)
        }
        if (result.isFailure){
            _mainUiEvents.emit(MainUiEvent.InitFailed)
            return false
        }
        _mainUiEvents.emit(MainUiEvent.InitSuccess)
        _pathState.update { it.copy(
            startLabel = LabelObject(
                entryNumber,
                OrientatedPosition(position, newOrientation)
            )
            ) }
        return true
    }

    private fun preload(){
        viewModelScope.launch {
            tree.preload()
        }
    }
}
