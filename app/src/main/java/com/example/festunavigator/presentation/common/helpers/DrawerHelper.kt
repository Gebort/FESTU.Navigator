package com.example.festunavigator.presentation.common.helpers

import android.graphics.Color
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.festunavigator.App
import com.gerbort.app.R
import com.gerbort.common.model.OrientatedPosition
import com.gerbort.common.model.TreeNode
import com.google.ar.core.Anchor
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.uchuhimo.collections.MutableBiMap
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.destroy
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.NodeParent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch


class DrawerHelper(
    private val fragment: Fragment,
    ) {

    private var labelScale = Scale(0.15f, 0.075f, 0.15f)
    private var arrowScale = Scale(0.5f, 0.5f, 0.5f)
    private var pathScale = Scale(0.1f)
    private var entryScale = Scale(0.05f)
    private var selectionPathScale = Scale(0.2f)
    private var selectionEntryScale = Scale(0.1f)
    private val pathModel = "models/cylinder.glb"
    private val entryModel = "models/box.glb"
    private val selectionModel = "models/cone.glb"
    private var labelAnimationDelay = 2L
    private var arrowAnimationDelay = 2L
    private var labelAnimationPart = 10
    private var arrowAnimationPart = 15
    private val bias = 0.15f

    private val animationJobs = mutableMapOf<ArNode, Job>()

    suspend fun drawNode(
        treeNode: TreeNode,
        parentNode: NodeParent,
        anchor: Anchor? = null
    ): ArNode {
        return when (treeNode){
            is TreeNode.Path -> {
                drawPath(treeNode, parentNode, anchor)
            }
            is TreeNode.Entry -> {
                drawEntry(treeNode, parentNode, anchor)
            }
        }

    }

    fun removeLink(
        pair: Pair<ArNode, ArNode>,
        modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode>
    ) {
        modelsToLinkModels[pair]?.destroy()
        modelsToLinkModels.remove(pair)
    }

    fun removeNode(
        treeNode: TreeNode,
        modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode>,
        treeNodesToModels: MutableBiMap<TreeNode, ArNode>,
    ){
        treeNodesToModels[treeNode]?.let { node ->
            modelsToLinkModels.keys
                .filter { pair ->
                    pair.first == node || pair.second == node
                }
                .forEach { pair ->
                    removeLink(pair, modelsToLinkModels)
                }
            treeNodesToModels.remove(treeNode)
            node.destroy()
            node.anchor?.destroy()
        }
    }

    suspend fun drawSelection(
        treeNode: TreeNode,
        parentNode: NodeParent,
    ): ArNode = when (treeNode) {
        is TreeNode.Entry -> {
            drawArNode(selectionModel, selectionEntryScale, treeNode.position, treeNode.forwardVector, parentNode, null, null)
        }
        is TreeNode.Path -> {
            drawArNode(selectionModel, selectionPathScale, treeNode.position, null, parentNode, null, null)
        }
    }

    private suspend fun drawPath(
        treeNode: TreeNode.Path,
        parentNode: NodeParent,
        anchor: Anchor? = null
    ): ArNode = drawArNode(pathModel, pathScale, treeNode.position, null, parentNode, anchor, treeNode.northDirection)

    private suspend fun drawArNode(
        model: String,
        scale: Scale,
        position: Float3,
        orientation: dev.romainguy.kotlin.math.Quaternion?,
        parentNode: NodeParent,
        anchor: Anchor?,
        northDirection: dev.romainguy.kotlin.math.Quaternion?
    ): ArNode {
        val modelNode = ArModelNode().apply {
            loadModel(
                context = fragment.requireContext(),
                lifecycle = fragment.lifecycle,
                glbFileLocation = model,
            )
            this.position = position
            orientation?.let {
                quaternion = it
            }
            modelScale = scale
            followHitPosition = false
            if (anchor != null){
                this.anchor = anchor
            }
            else {
                this.anchor = createAnchor()
            }
            this.model?.let {
                it.isShadowCaster = false
                it.isShadowReceiver = false
            }
        }

        parentNode.addChild(modelNode)

        return modelNode
    }

    private suspend fun drawEntry(
        treeNode: TreeNode.Entry,
        parentNode: NodeParent,
        anchor: Anchor? = null
    ): ArNode {
        return placeLabel(
            treeNode.number,
            OrientatedPosition(treeNode.position, treeNode.forwardVector),
            parentNode
        ).apply {
            if (App.isAdmin) {
                addChild(
                    drawArNode(
                        model = entryModel,
                        scale = entryScale,
                        position = Position(0f, -bias, 0f),
                        orientation = dev.romainguy.kotlin.math.Quaternion(),
                        parentNode = parentNode,
                        anchor = anchor,
                        northDirection = treeNode.northDirection
                    )
                )
            }
        }

    }

    suspend fun placeLabel(
        label: String,
        pos: OrientatedPosition,
        parentNode: NodeParent,
        anchor: Anchor? = null
    ): ArNode = placeRend(
        label = label,
        pos = pos,
        parentNode = parentNode,
        scale = labelScale,
        anchor = anchor
    )

    suspend fun placeArrow(
        pos: OrientatedPosition,
        parentNode: NodeParent
    ): ArNode = placeRend(
        pos = pos,
        parentNode = parentNode,
        scale = arrowScale
    )

    private suspend fun placeRend(
        label: String? = null,
        pos: OrientatedPosition,
        parentNode: NodeParent,
        scale: Scale,
        anchor: Anchor? = null
    ): ArNode {
        var node: ArNode? = null
        ViewRenderable.builder()
            .setView(fragment.requireContext(), if (label != null) R.layout.text_sign else R.layout.route_node)
            .setSizer { Vector3(0f, 0f, 0f) }
            .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
            .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
            .build(fragment.lifecycle)
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
                val textNode = ArModelNode().apply {
                    followHitPosition = false
                    setModel(
                        renderable = renderable
                    )
                    position = pos.position
                    quaternion = pos.orientation
                    if (anchor != null){
                        this.anchor = anchor
                    }
                    else {
                        this.anchor = this.createAnchor()
                    }
                }
                parentNode.addChild(textNode)
                node = textNode
                textNode.animateViewAppear(
                    scale,
                    if (label != null) labelAnimationDelay else arrowAnimationDelay,
                    if (label != null) labelAnimationPart else arrowAnimationPart
                )
            }
            .await()

        return node!!
    }

    fun removeNode(node: ArNode) {
        animationJobs[node]?.cancel()
        node.destroy()
        node.anchor?.destroy()
    }

    fun removeArrowWithAnim(node: ArNode) {
        node.model as ViewRenderable? ?: throw Exception("No view renderable")
        node.animateViewDisappear(arrowScale, arrowAnimationDelay, arrowAnimationPart)
    }

    fun removeLabelWithAnim(node: ArNode) {
        node.model as ViewRenderable? ?: throw Exception("No view renderable")
        node.animateViewDisappear(arrowScale, labelAnimationDelay, labelAnimationPart)
    }

    suspend fun drawLine(
        from: Position,
        to: Position,
        parentNode: NodeParent
    ): ArNode {
        val fromVector = from.toVector3()
        val toVector = to.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = com.google.ar.sceneform.rendering.Color(Color.parseColor("#ffffff"))

        // 1. make a material by the color
        val node = ArNode()
        MaterialFactory.makeOpaqueWithColor(fragment.requireContext(), fragment.lifecycle, colorOrange)
            .thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    fragment.lifecycle,
                    0.01f, lineLength,
                    Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                node.setModel(model)
                //from.addChild(node)
                //node.anchor = from.anchor
                parentNode.addChild(node)

                // 4. set rotation
                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB = Quaternion.lookRotation(
                        directionFromTopToBottom,
                        Vector3.up()
                    )

                val rotation = Quaternion.multiply(
                    rotationFromAToB,
                    Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                )

                node.quaternion = rotation.toNewQuaternion()
                node.position = from
            }.await()
        return node
    }

    private fun ArNode.animateViewAppear(targetScale: Scale, delay: Long, part: Int) {
        animateView(true, targetScale, delay, part, end = null)
    }

    private fun ArNode.animateViewDisappear(targetScale: Scale, delay: Long, part: Int) {
        animateView(false, targetScale, delay, part) {
            removeNode(it)
        }
    }

    private fun ArNode.animateView(appear: Boolean, targetScale: Scale, delay: Long, part: Int, end: ((ArNode) -> Unit)?) {
        val renderable = this.model as ViewRenderable? ?: throw Exception("No view renderable")
        var size = renderable.sizer.getSize(renderable.view)
        val xPart = targetScale.x/part
        val yPart = targetScale.y/part
        val zPart = targetScale.z/part
        animationJobs[this]?.cancel()
        animationJobs[this] = fragment.viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (size.x >= targetScale.toVector3().x && appear) {
                    break
                }
                else if(size.x <= 0 && !appear){
                    break
                }
                renderable.sizer = ViewSizer {
                    if (appear)
                        size.addConst(xPart, yPart, zPart)
                    else
                        size.addConst(xPart, yPart, zPart, -1)

                }
                delay(delay)
                size = renderable.sizer.getSize(renderable.view)
            }
            if (end != null) {
                end(this@animateView)
            }
        }
    }

    suspend fun placeBlankNode(
        parentNode: NodeParent,
        position: Float3? = null,
        anchor: Anchor? = null
    ): ArNode {
        val node = ArModelNode().apply {
            position?.let {
            this.position = it
            }
            followHitPosition = false
            if (anchor != null){
                this.anchor = anchor
            }
            else {
                this.anchor = createAnchor()
            }
        }
        parentNode.addChild(node)
        return node
    }

    suspend fun joinAnimation(node: ArNode) {
        animationJobs[node]?.join()
    }

    private fun Vector3.addConst(xValue: Float, yValue: Float, zValue: Float, modifier: Int = 1): Vector3 {
        return Vector3(
            x + xValue * modifier,
            y + yValue * modifier,
            z + zValue * modifier
        )
    }
}