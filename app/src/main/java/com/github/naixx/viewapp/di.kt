package com.github.naixx.viewapp

import com.github.naixx.viewapp.utils.PrefsStorage
import github.naixx.network.StorageProvider
import org.koin.dsl.module

val appModule = module {
    single<StorageProvider> {
        PrefsStorage()
    }
}
