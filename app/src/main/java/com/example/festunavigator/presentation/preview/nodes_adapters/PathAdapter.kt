package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.presentation.common.helpers.DrawerHelper
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow


class PathAdapter(
    drawerHelper: DrawerHelper,
    previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
): NodesAdapter<OrientatedPosition>(drawerHelper, previewView, bufferSize, scope)
{

    override suspend fun onInserted(item: OrientatedPosition): ArNode =
        drawerHelper.placeArrow(item, previewView)

    override suspend fun onRemoved(item: OrientatedPosition, node: ArNode) =
        drawerHelper.removeArrowWithAnim(node)
}