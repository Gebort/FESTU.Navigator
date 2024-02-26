package com.example.festunavigator.presentation.preview

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.festunavigator.presentation.preview.state.PathState
import com.example.festunavigator.presentation.search.SearchFragment
import com.example.festunavigator.presentation.search.SearchUiEvent
import com.gerbort.common.model.Record
import com.gerbort.common.model.TreeNode
import com.gerbort.common.utils.fromVector
import com.gerbort.common.utils.multiply
import com.gerbort.common.utils.reverseConvertPosition
import com.gerbort.common.utils.rotateBy
import com.gerbort.data.domain.repositories.RecordsRepository
import com.gerbort.hit_test.HitTestResult
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.google.ar.sceneform.math.Vector3
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.RotationsOrder
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainShareModel @Inject constructor(
    private val nodeGraph: NodeGraph,
): ViewModel() {

    private var _frame = MutableStateFlow<ArFrame?>(null)
    val frame = _frame.asStateFlow()

    var northLocation: Float3? = null
        private set

    val treeDiffUtils = nodeGraph.getDiffUtils()


    init {
        preload()
    }

    fun onEvent(event: MainEvent) {
        when (event){
            is MainEvent.NewAzimuth -> {
                newNorthLocation(event.azimuthRadians)
            }
            is MainEvent.PivotTransform -> {
                viewModelScope.launch {
                    _treePivot.value?.let { tp ->
                        _treePivot.update { tp.copy(orientation = tp.orientation.multiply(event.transition)) }
                    }
                }
            }
        }
    }

    private fun newNorthLocation(azimuth: Float) {
        frame.value?.let {
            val rotation = Quaternion.fromEuler(yaw = azimuth, order = RotationsOrder.ZYX)
            val cameraDirection = it.camera.pose.rotation.copy(z = 0f).toQuaternion()
            val northDirection = cameraDirection * rotation
            this.northLocation = Vector3(Float.MAX_VALUE, 0f, 0f).rotateBy(northDirection).toFloat3()
        }
    }


    private fun preload(){
        viewModelScope.launch {
            nodeGraph.preload()
        }
    }
}
