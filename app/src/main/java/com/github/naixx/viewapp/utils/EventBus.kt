

package com.github.naixx.viewapp.utils

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dokar.sonner.*
import com.github.naixx.logger.LL
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlin.to

private typealias Event = Pair<String, ToastType>

object EventBus {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    fun message(message: String) {
        LL.i("[[$message]]]")
        _events.tryEmit(message to ToastType.Normal)
    }

    fun error(message: String) {
        LL.i("[[$message]]]")
        _events.tryEmit(message to ToastType.Error)
    }

    fun info(message: String) {
        LL.i("[[$message]]]")
        _events.tryEmit(message to ToastType.Info)
    }

    fun warning(message: String) {
        LL.i("[[$message]]]")
        _events.tryEmit(message to ToastType.Warning)
    }

    fun success(message: String) {
        LL.i("[[$message]]]")
        _events.tryEmit(message to ToastType.Success)
    }
}

@Composable
fun ToastMessageHandler() {
    val toaster = rememberToasterState()
    Toaster(
        state = toaster,
        contentColor = {
            LightToastColors[it.type]!!.content
        },
        richColors = true,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    )
    LaunchedEffect(Unit) {
        toaster.listen(
            EventBus.events.map {
                Toast(it.first, type = it.second)
            }
        )
    }
}

private class ToastColors(
    val background: Color,
    val content: Color,
    val border: Color,
)

private val LightToastColors = mapOf(
    ToastType.Normal to ToastColors(
        background = Color.White,
        content = Color(0xff171717),
        border = Color(0xffededed),
    ),
    ToastType.Success to ToastColors(
        background = Color(0xffecfdf3),
        content = Color(0xFF245933),
        border = Color(0xffd3fde5),
    ),
    ToastType.Info to ToastColors(
        background = Color(0xfff0f8ff),
        content = Color.Black,
        border = Color.White,
    ),
    ToastType.Warning to ToastColors(
        background = Color(0xfffffcf0),
        content = Color(0xFFB77900),
        border = Color(0xfffdf5d3),
    ),
    ToastType.Error to ToastColors(
        background = Color(0xfffff0f0),
        content = Color(0xFF880F07),
        border = Color(0xffffe0e1),
    ),
)
