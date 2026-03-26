package com.conkeep.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEventBus
    @Inject
    constructor() {
        private val _logoutEvent = MutableSharedFlow<Unit>()
        val logoutEvent = _logoutEvent.asSharedFlow()

        suspend fun emitForceLogout() {
            _logoutEvent.emit(Unit)
        }
    }
