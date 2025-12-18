package com.jetbrains.kmpapp.di

import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.jetbrains.kmpapp.data.MusicRepository
import com.jetbrains.kmpapp.screens.detail.SongDetailViewModel
import com.jetbrains.kmpapp.screens.list.SongsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    single {
        MusicRepository()
    }
}

val viewModelModule = module {
    factoryOf(::ErrorManager)
    factoryOf(::SongsViewModel)
    factoryOf(::SongDetailViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
        )
    }
}
