package com.adamczewski.kmpmvi.sample.di

import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.screens.detail.SongDetailViewModel
import com.adamczewski.kmpmvi.sample.screens.list.SongsViewModel
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
