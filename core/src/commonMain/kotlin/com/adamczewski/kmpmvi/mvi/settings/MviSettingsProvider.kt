package com.adamczewski.kmpmvi.mvi.settings

import kotlin.reflect.KClass

public fun interface MviSettingsProvider {
    public fun provide(tag: String, klass: KClass<out Any>): MviSettings
}

public fun buildMviSettingsProvider(
    block: MviSettingsBuilder.(tag: String, klass: KClass<out Any>) -> Unit
): MviSettingsProvider = MviSettingsProvider { tag, klass ->
    val default = DefaultMviSettingsProvider.provide(tag, klass)
    buildMviSettings(default) { block(tag, klass) }
}
