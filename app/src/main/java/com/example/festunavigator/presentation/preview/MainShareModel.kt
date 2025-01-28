package com.example.festunavigator.presentation.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gerbort.node_graph.domain.graph.NodeGraph
import com.gerbort.node_graph.domain.graph.SingleLinksChangeListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainShareModel @Inject constructor(
    private val nodeGraph: NodeGraph,
): ViewModel() {

    private val _uiEvent = Channel<MainUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    var northLocation: Float3? = null
        private set

    val treeDiffUtils = nodeGraph.getDiffUtils()


    init {
        preload()
        viewModelScope.launch {
            nodeGraph.getPositionData().collect { positionData ->
                if (nodeGraph.isInitialized()) _uiEvent.send(MainUiEvent.GraphPositionChanged(positionData))
            }
        }

    }

    fun onEvent(event: MainEvent) {
        when (event){
            is MainEvent.NewAzimuth -> {
              //  newNorthLocation(event.azimuthRadians)
            }
            is MainEvent.PivotTransform -> {
                viewModelScope.launch {
                    nodeGraph.updateTreePivot(event.transition)
                }
            }
        }
    }

    fun setSingleLinksChangeListener(listener: SingleLinksChangeListener) {
        nodeGraph.setChangedLinksListener(listener)
    }

//    private fun newNorthLocation(azimuth: Float) {
//        frame.value?.let {
//            val rotation = Quaternion.fromEuler(yaw = azimuth, order = RotationsOrder.ZYX)
//            val cameraDirection = it.camera.pose.rotation.copy(z = 0f).toQuaternion()
//            val northDirection = cameraDirection * rotation
//            this.northLocation = Vector3(Float.MAX_VALUE, 0f, 0f).rotateBy(northDirection).toFloat3()
//        }
//    }


    private fun preload(){
        viewModelScope.launch {
            nodeGraph.preload()
        }
    }
}
