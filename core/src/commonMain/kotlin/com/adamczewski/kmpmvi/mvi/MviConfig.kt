package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettingsProvider

public object MviConfig {
    public var settingsProvider: MviSettingsProvider = DefaultMviSettingsProvider
}
