package com.jetbrains.kmpapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jetbrains.kmpapp.di.initKoin
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        App()
    }
}
