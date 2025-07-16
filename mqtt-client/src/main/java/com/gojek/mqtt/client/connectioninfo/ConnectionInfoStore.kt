package com.gojek.mqtt.client.connectioninfo

internal object ConnectionInfoStore {
    @Volatile
    private var state: State = State.UninitialisedState

    fun getConnectionInfo(): ConnectionInfo? {
        return state.connectionInfo
    }

    fun updateConnectionInfo(connectionInfo: ConnectionInfo) {
        state = State.InitialisedState(connectionInfo)
    }

    fun removeConnectionInfo() {
        state = State.UninitialisedState
    }
}

private sealed class State(
    open val connectionInfo: ConnectionInfo? = null
) {
    object UninitialisedState : State()
    class InitialisedState(
        override val connectionInfo: ConnectionInfo
    ) : State(connectionInfo)
}
