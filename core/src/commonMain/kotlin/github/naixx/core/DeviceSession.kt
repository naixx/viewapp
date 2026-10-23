package github.naixx.core

import github.naixx.network.BaseMessage
import github.naixx.network.ConnectionState
import github.naixx.network.OutMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one connection to a VIEW, as the shells provide it: on Android the foreground service that keeps the
 * WebSocket alive in the background, in the browser the client loop of the page. Only transport — what the
 * device said is interpreted by [DeviceState].
 */
interface DeviceSession {

    val state: StateFlow<ConnectionState>

    /** Every frame the device sends, in order. */
    val messages: SharedFlow<BaseMessage>

    fun send(message: OutMessage)

    /** Whether the user can start and stop the connection here (the Android service: yes; the browser: no). */
    val controllable: Boolean

    fun connect()

    fun disconnect()
}
