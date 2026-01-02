package com.adamczewski.kmpmvi.mvi.settings

import kotlin.reflect.KClass

public fun interface MviSettingsProvider {
    public fun provide(tag: String, klass: KClass<out Any>): MviSettings
}
