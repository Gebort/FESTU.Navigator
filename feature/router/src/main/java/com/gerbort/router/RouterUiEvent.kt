package com.gerbort.router

import com.gerbort.common.model.TreeNode
import com.google.ar.core.Anchor

sealed interface RouterUiEvent {

    data object LinkAlreadyExists: RouterUiEvent
    data object NodeCreated: RouterUiEvent
    data object NodeAlreadyExists: RouterUiEvent
    class LinkCreated(val node1: TreeNode, val node2: TreeNode): RouterUiEvent
    class NodeDeleted(val node: TreeNode): RouterUiEvent
}