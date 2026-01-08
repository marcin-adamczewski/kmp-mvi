package com.adamczewski.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.adamczewski.kmpmvi.sample.App
import com.adamczewski.kmpmvi.sample.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        App()
    }
}
