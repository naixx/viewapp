package com.github.naixx.viewapp

import com.github.naixx.viewapp.ui.MainViewModel
import com.github.naixx.viewapp.ui.TimelapseViewModel
import com.github.naixx.viewapp.utils.PrefsStorage
import github.naixx.network.Clip
import github.naixx.network.StorageProvider
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<StorageProvider> {
        PrefsStorage()
    }
    viewModel { MainViewModel() }
    viewModel { (clip: Clip) -> TimelapseViewModel(clip) }
}
