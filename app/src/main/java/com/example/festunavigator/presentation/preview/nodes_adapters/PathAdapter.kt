package com.example.festunavigator.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.gerbort.common.model.OrientatedPosition
import com.gerbort.core_ui.drawer_helper.DrawerHelper
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode


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