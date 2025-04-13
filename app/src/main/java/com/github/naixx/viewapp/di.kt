package com.github.naixx.viewapp

import com.github.naixx.viewapp.ui.*
import com.github.naixx.viewapp.utils.PrefsStorage
import github.naixx.network.*
import org.koin.androidx.viewmodel.dsl.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf<StorageProvider>(::PrefsStorage)
    viewModelOf(::MainViewModel)
    viewModel { (clip: Clip) -> TimelapseViewModel(clip) }
}
