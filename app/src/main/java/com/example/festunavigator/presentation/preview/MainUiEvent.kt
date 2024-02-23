package com.example.festunavigator.presentation.preview

import com.gerbort.common.model.TreeNode
import com.google.ar.core.Anchor
import java.lang.Exception

sealed interface MainUiEvent {
    class InitSuccess(val initialEntry: TreeNode.Entry?): MainUiEvent
    class InitFailed(val error: Exception? = null): MainUiEvent
    object PathNotFound: MainUiEvent
    object EntryAlreadyExists: MainUiEvent
    object EntryCreated: MainUiEvent
    object LinkAlreadyExists: MainUiEvent
    class NodeCreated(val treeNode: TreeNode, val anchor: Anchor? = null): MainUiEvent
    class LinkCreated(val node1: TreeNode, val node2: TreeNode): MainUiEvent
    class NodeDeleted(val node: TreeNode): MainUiEvent
}