package com.adamczewski.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.adamczewski.kmpmvi.sample.App
import com.adamczewski.kmpmvi.sample.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KMP MVI Sample",
        ) {
            App()
        }
    }
}
