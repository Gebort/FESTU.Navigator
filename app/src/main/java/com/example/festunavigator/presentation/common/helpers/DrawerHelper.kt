package com.example.festunavigator.presentation.common.helpers

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.presentation.MainActivity
import com.google.ar.core.Anchor
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.uchuhimo.collections.MutableBiMap
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.Node
import kotlinx.coroutines.launch

class DrawerHelper(
    private val activity: MainActivity
) {

    val step = 1f

    suspend fun drawNode(
        treeNode: TreeNode,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        surfaceView: ArSceneView,
        anchor: Anchor? = null
    ){
        val modelNode = ArNode()
        modelNode.loadModel(
            context = activity.applicationContext,
            glbFileLocation = if (treeNode is TreeNode.Entry) "models/cylinder_green.glb" else "models/cylinder.glb",
        )
        modelNode.position = treeNode.position
        modelNode.modelScale = Scale(0.1f)
        modelNode.anchor = modelNode.createAnchor()
//        anchor?.let {
//            modelNode.anchor = it
//        }
        modelNode.model?.let {
            it.isShadowCaster = false
            it.isShadowReceiver = false
        }

        treeNodesToModels[treeNode] = modelNode

        surfaceView.addChild(modelNode)
    }

    fun drawWay(
        nodes: List<TreeNode>,
        linksToWayModels: MutableBiMap<Pair<Node, Node>, Node>,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        surfaceView: ArSceneView
    ){
        linksToWayModels.values.forEach { it.destroy() }
        linksToWayModels.clear()


        if (nodes.size > 1){
            for (i in 1 until nodes.size) {
                val node1 = treeNodesToModels[nodes[i-1]]
                val node2 = treeNodesToModels[nodes[i]]

                if (node1 != null && node2 != null) {
                    drawWayLine(
                        node1,
                        node2,
                        linksToWayModels,
                        surfaceView
                    )
                }
            }
        }

    }

    fun drawTree(
        tree: Tree,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node>,
        surfaceView: ArSceneView
    ){
        activity.lifecycleScope.launch {
            for (node in tree.allPoints.values){
                drawNode(
                    node,
                    treeNodesToModels,
                    surfaceView
                )
            }
            for (treenode1 in tree.links.keys){
                val node1 = treeNodesToModels[treenode1]!!
                val others = tree.links[treenode1]!!
                for (treenode2 in others) {
                    val node2 = treeNodesToModels[treenode2]!!
                    if (modelsToLinkModels[Pair(node1, node2)] == null ){
                        drawLine(
                            node1,
                            node2,
                            modelsToLinkModels,
                            surfaceView
                        )
                    }
                }
            }
        }
    }

    fun drawWayLine(
        from: Node,
        to: Node,
        linksToWayModels: MutableBiMap<Pair<Node, Node>, Node>,
        surfaceView: ArSceneView
    ){

        val fromVector = from.position.toVector3()
        val toVector = to.position.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = com.google.ar.sceneform.rendering.Color(Color.parseColor("#7cfc00"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(activity.applicationContext, colorOrange)
            .thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    0.015f, lineLength,
                    Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                val node = ArNode()
                node.setModel(model)
                node.parent = from
                surfaceView.addChild(node)

                // 4. set rotation
                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB: Quaternion =
                    Quaternion.lookRotation(
                        directionFromTopToBottom,
                        Vector3.up()
                    )

                val rotation = Quaternion.multiply(
                    rotationFromAToB,
                    Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                )

                node.modelQuaternion = dev.romainguy.kotlin.math.Quaternion(
                    rotation.x,
                    rotation.y,
                    rotation.z,
                    rotation.w
                )
                node.position = from.position

                linksToWayModels[Pair(from, to)] = node
            }
    }

    fun drawLine(
        from: Node,
        to: Node,
        modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node>,
        surfaceView: ArSceneView
    ){

        val fromVector = from.position.toVector3()
        val toVector = to.position.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = com.google.ar.sceneform.rendering.Color(Color.parseColor("#ffffff"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(activity.applicationContext, colorOrange)
            .thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    0.01f, lineLength,
                    Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                val node = ArNode()
                node.setModel(model)
                node.parent = from
                surfaceView.addChild(node)

                // 4. set rotation
                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB: Quaternion =
                    Quaternion.lookRotation(
                        directionFromTopToBottom,
                        Vector3.up()
                    )

                val rotation = Quaternion.multiply(
                    rotationFromAToB,
                    Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                )

                node.modelQuaternion = dev.romainguy.kotlin.math.Quaternion(
                    rotation.x,
                    rotation.y,
                    rotation.z,
                    rotation.w
                )
                node.position = from.position

                modelsToLinkModels[Pair(from, to)] = node
            }
    }


}