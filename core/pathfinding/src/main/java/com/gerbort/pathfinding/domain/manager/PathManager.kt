package com.gerbort.pathfinding.domain.manager

import kotlinx.coroutines.flow.Flow

interface PathManager {

    fun getPathState(): Flow<PathState>

    fun setStart(label: String?)

    fun setEnd(label: String?)

    fun setStartAndEnd(startLabel: String?, endLabel: String?)

}