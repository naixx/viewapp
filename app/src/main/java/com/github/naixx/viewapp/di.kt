package com.github.naixx.viewapp

import com.github.naixx.viewapp.ui.*
import com.github.naixx.viewapp.utils.PrefsStorage
import github.naixx.core.DeviceSession
import github.naixx.core.DeviceState
import github.naixx.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf<StorageProvider>(::PrefsStorage)
    // The connection and the typed device state live as long as the process (see docs/2026-10-24-shared-ui-architecture.md).
    single<DeviceSession> { AndroidDeviceSession(androidContext(), get()) }
    single { DeviceState(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    viewModelOf(::MainViewModel)
    viewModelOf(::ProgramViewModel)
    viewModel { (clip: Clip) -> TimelapseViewModel(clip) }
}
