package com.gerbort.core_ui.tap_flow

import com.gerbort.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class UserTapHolder(
    @ApplicationScope private val appScope: CoroutineScope
): UserTapConsumer, UserTapProducer {

    private val _taps = MutableSharedFlow<UserTap>()

    override fun newTap(userTap: UserTap) {
        appScope.launch {
            _taps.emit(userTap)
        }
    }

    override fun getUserTaps(): Flow<UserTap> = _taps.asSharedFlow()


}