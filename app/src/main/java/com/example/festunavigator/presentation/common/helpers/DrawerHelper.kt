package com.example.festunavigator.presentation.common.helpers

import android.graphics.Color
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.festunavigator.R
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.example.festunavigator.domain.tree.Tree
import com.example.festunavigator.domain.tree.TreeNode
import com.example.festunavigator.domain.use_cases.SmoothPath
import com.example.festunavigator.presentation.MainActivity
import com.google.ar.core.Anchor
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.uchuhimo.collections.MutableBiMap
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.*
import io.github.sceneview.node.Node
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class DrawerHelper(
    private val activity: MainActivity,
    private val smoothPath: SmoothPath = SmoothPath()
) {

    private val routeStep = 0.2f
    private var labelScale = Scale(0.15f, 0.075f, 0.15f)
    private var arrowScale = Scale(0.5f, 0.5f, 0.5f)
    private var treeDrawingJob: Job? = null

    suspend fun drawNode(
        treeNode: TreeNode,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        surfaceView: ArSceneView,
        anchor: Anchor? = null
    ){
        when (treeNode){
            is TreeNode.Path -> {
                drawPath(treeNode, treeNodesToModels, surfaceView, anchor)
            }
            is TreeNode.Entry -> {
                drawEntry(treeNode, treeNodesToModels, surfaceView, anchor)
            }
            else -> {
                throw Exception("Unknown treeNode type")
            }
        }

    }

    private suspend fun drawPath(
        treeNode: TreeNode.Path,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        surfaceView: ArSceneView,
        anchor: Anchor? = null
    ){
        val modelNode = ArNode()
        modelNode.loadModel(
            context = activity.applicationContext,
            glbFileLocation = "models/cylinder.glb",
        )
        modelNode.position = treeNode.position
        modelNode.modelScale = Scale(0.1f)
        if (anchor != null){
            modelNode.anchor = anchor
        }
        else {
            modelNode.anchor = modelNode.createAnchor()
        }
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

    private suspend fun drawEntry(
        treeNode: TreeNode.Entry,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        surfaceView: ArSceneView,
        anchor: Anchor? = null
    ){
        val modelNode = placeLabel(
            treeNode.number,
            OrientatedPosition(treeNode.position, treeNode.forwardVector),
            surfaceView
        )
        treeNodesToModels[treeNode] = modelNode
    }

    fun drawTree(
        tree: Tree,
        treeNodesToModels: MutableBiMap<TreeNode, Node>,
        modelsToLinkModels: MutableBiMap<Pair<Node, Node>, Node>,
        surfaceView: ArSceneView
    ){
        treeDrawingJob?.cancel()
        treeDrawingJob = activity.lifecycleScope.launch {
            for (node in tree.getAllNodes()){
                drawNode(
                    node,
                    treeNodesToModels,
                    surfaceView
                )
                yield()
            }
            for (treenode1 in tree.getNodesWithLinks()){
                val node1 = treeNodesToModels[treenode1]!!
                val others = tree.getNodeLinks(treenode1)!!
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
                yield()
            }
        }
    }

    suspend fun drawWay(
        nodes: List<TreeNode>,
        routeLabels: MutableList<ArNode>,
        surfaceView: ArSceneView
    ){

        routeLabels.forEach { it.destroy() }
        routeLabels.clear()

        val way = smoothPath(nodes)

        for (pos in way) {
            routeLabels.add(placeArrow(pos, surfaceView))
            yield()
        }

    }

    suspend fun placeLabel(
        label: String,
        pos: OrientatedPosition,
        surfaceView: ArSceneView,
        anchor: Anchor? = null
    ): ArNode = placeRend(
        label = label,
        pos = pos,
        surfaceView = surfaceView,
        scale = labelScale,
        anchor = anchor
    )

    private suspend fun placeArrow(
        pos: OrientatedPosition,
        surfaceView: ArSceneView
    ): ArNode = placeRend(
        pos = pos,
        surfaceView = surfaceView,
        scale = arrowScale
    )

    private suspend fun placeRend(
        label: String? = null,
        pos: OrientatedPosition,
        surfaceView: ArSceneView,
        scale: Scale,
        anchor: Anchor? = null
    ): ArNode {
        var node: ArNode? = null
        ViewRenderable.builder()
            .setView(activity, if (label != null) R.layout.text_sign else R.layout.route_node)
            .setSizer { scale.toVector3() }
            .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
            .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
            .build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.let {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                }
                if (label != null) {
                    val cardView = renderable.view as CardView
                    val textView: TextView = cardView.findViewById(R.id.signTextView)
                    textView.text = label
                }
                val textNode = ArNode().apply {
                    setModel(
                        renderable = renderable
                    )
                    model
                    position = Position(pos.position.x, pos.position.y, pos.position.z)
                    quaternion = pos.orientation
                    if (anchor != null){
                        this.anchor = anchor
                    }
                    else {
                        this.anchor = this.createAnchor()
                    }
                }

                surfaceView.addChild(textNode)
                node = textNode
            }
            .await()

        return node!!
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

                node.modelQuaternion = rotation.toNewQuaternion()
                node.position = from.position

                modelsToLinkModels[Pair(from, to)] = node
            }
    }


}