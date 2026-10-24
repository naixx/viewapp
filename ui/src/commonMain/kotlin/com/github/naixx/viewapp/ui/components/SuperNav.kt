package com.github.naixx.viewapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import cafe.adriel.voyager.navigator.Navigator

//this is a special top level navigator to open full size screens that can inject "activity-like" view models
val SuperNav: Navigator?
    @Composable get() = if (LocalInspectionMode.current) null else LocalSuperNavigator.current
val LocalSuperNavigator: ProvidableCompositionLocal<Navigator?> =
    staticCompositionLocalOf { null }
