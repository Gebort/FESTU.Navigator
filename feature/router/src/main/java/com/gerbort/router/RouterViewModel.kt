package com.gerbort.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gerbort.common.utils.fromVector
import com.gerbort.node_graph.domain.use_cases.CreateNodeUseCase
import com.gerbort.node_graph.domain.use_cases.DeleteNodeUseCase
import com.gerbort.node_graph.domain.use_cases.LinkNodesUseCase
import com.gerbort.pathfinding.domain.manager.PathManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouterViewModel @Inject constructor(
    private val deleteNodeUseCase: DeleteNodeUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    private val linkNodesUseCase: LinkNodesUseCase,
): ViewModel() {

    private val _state = MutableStateFlow(RouterState())
    val state = _state.asStateFlow()

    private val _uiEvents = Channel<RouterUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onEvent(event: RouterEvent) {
        viewModelScope.launch {
            when (event) {
                is RouterEvent.ChangeLinkMode -> _state.update { it.copy(linkPlacement = !state.value.linkPlacement) }

                //TODO North direction
//                    val northDirection = when (north) {
//                    null -> null
//                    else -> Quaternion.fromVector((north - position2).toVector3().normalized())
//                }

                is RouterEvent.CreatePathNode -> createNodeUseCase(position = event.position).fold(
                    onSuccess = { _uiEvents.send(RouterUiEvent.NodeCreated) },
                    onFailure = { _uiEvents.send(RouterUiEvent.NodeAlreadyExists) }
                )

                is RouterEvent.DeleteNode -> deleteNodeUseCase(event.node).let {
                    _uiEvents.send(RouterUiEvent.NodeDeleted(event.node))
                }

                is RouterEvent.LinkNodes -> linkNodesUseCase(event.node1, event.node2).let {
                    if (it) {
                        _state.update { it.copy(linkPlacement = false) }
                        _uiEvents.send(RouterUiEvent.LinkCreated(event.node1, event.node2))
                    }
                }
            }
        }
    }

}