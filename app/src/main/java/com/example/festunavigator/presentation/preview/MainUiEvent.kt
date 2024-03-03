package com.example.festunavigator.presentation.preview

import com.gerbort.node_graph.domain.graph.NodeGraphPosition

sealed interface MainUiEvent {
    class GraphPositionChanged(val nodeGraphPosition: NodeGraphPosition): MainUiEvent

}
