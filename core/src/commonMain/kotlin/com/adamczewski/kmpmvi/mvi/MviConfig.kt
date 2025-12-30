package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettingsProvider

object MviConfig {
    var canLog: Boolean = true
    var settingsProvider: MviSettingsProvider = DefaultMviSettingsProvider
}
