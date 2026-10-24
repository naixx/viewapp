package com.github.naixx.viewapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.naixx.viewapp.utils.EventBus
import github.naixx.core.AppNotification
import github.naixx.core.DeviceState
import github.naixx.network.ConnectionState
import github.naixx.network.OutMessage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The screens' view of the device: a facade over the app-scoped [DeviceState] (which outlives this Activity-scoped
 * view model), kept so the screens keep their property names while they move to the shared :ui module.
 */
class MainViewModel(private val device: DeviceState) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> get() = device.connection

    val notifications: StateFlow<List<AppNotification>> get() = device.notifications

    val connectedMessage get() = device.camera
    val settingsMessage get() = device.settings
    val batteryMessage get() = device.battery
    val thumbnail get() = device.thumbnail
    val intervalometerStatus get() = device.intervalometerStatus
    val histogram get() = device.histogram
    val program get() = device.program

    val deviceClock get() = device.deviceClock

    init {
        // Capture errors are toasted while a UI is there to show them; the notification list keeps them anyway.
        device.errors.onEach { EventBus.error(it) }.launchIn(viewModelScope)
    }

    /** Mark every notification as read (called when the user opens the notification center). */
    fun markNotificationsRead() = device.markNotificationsRead()

    fun clearNotifications() = device.clearNotifications()

    fun send(message: OutMessage) = device.send(message)
}
