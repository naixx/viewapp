package com.github.naixx.viewapp.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
inline fun <reified T : ViewModel> activityViewModel(): T {
    val activity = LocalContext.current as ComponentActivity
    return koinViewModel<T>(viewModelStoreOwner = activity)
}
