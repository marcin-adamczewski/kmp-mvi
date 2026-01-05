package com.adamczewski.kmpmvi.mvi.settings

import kotlin.reflect.KClass

public fun interface MviSettingsProvider {
    public fun provide(tag: String, klass: KClass<out Any>): MviSettings
}

public object MviSettingProviderBuilder {
    public fun withDefaultSettings(
        settingsProvider: (defaultSettings: MviSettings, logTag: String, containerClass: KClass<out Any>) -> MviSettings
    ): MviSettingsProvider {
        return MviSettingsProvider { tag, klass ->
            settingsProvider(DefaultMviSettingsProvider.provide(tag, klass), tag, klass)
        }
    }

    public fun defaultProdiver(): MviSettingsProvider = DefaultMviSettingsProvider
}
