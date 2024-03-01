package com.gerbort.core_ui.tap_flow

import kotlinx.coroutines.flow.Flow

interface UserTapProducer {

    fun getUserTaps(): Flow<UserTap>

}