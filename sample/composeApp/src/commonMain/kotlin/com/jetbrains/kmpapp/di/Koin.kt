package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.MusicRepository
import com.jetbrains.kmpapp.screens.detail.DetailViewModel
import com.jetbrains.kmpapp.screens.list.ListViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    single {
        MusicRepository()
    }
}

val viewModelModule = module {
    factoryOf(::ListViewModel)
    factoryOf(::DetailViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
        )
    }
}
