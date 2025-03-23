package com.github.naixx.viewapp.di

import android.content.Context
import com.github.naixx.viewapp.encoding.VideoEncoderFactory
import com.github.naixx.viewapp.encoding.VideoEncoderFactoryImpl
import com.github.naixx.viewapp.encoding.VideoExportRepository
import com.github.naixx.viewapp.encoding.VideoExportRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val encodingModule = module {
    // Factory for creating VideoEncoder instances
    single<VideoEncoderFactory> { VideoEncoderFactoryImpl(androidContext()) }

    // Repository for exporting videos
    single<VideoExportRepository> { VideoExportRepositoryImpl(androidContext(), get()) }
}
