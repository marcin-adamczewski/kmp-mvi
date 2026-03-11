package com.adamczewski.kmpmvi.sample.di

import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.data.MusicRepositoryImpl
import com.adamczewski.kmpmvi.sample.screens.detail.SongDetailViewModel
import com.adamczewski.kmpmvi.sample.screens.dsl.list.SongsDslViewModel
import com.adamczewski.kmpmvi.sample.screens.list.SongsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single { MusicRepositoryImpl() } bind MusicRepository::class
}

val viewModelModule = module {
    factoryOf(::ErrorManager)
    factoryOf(::SongsViewModel)
    factoryOf(::SongsDslViewModel)
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
