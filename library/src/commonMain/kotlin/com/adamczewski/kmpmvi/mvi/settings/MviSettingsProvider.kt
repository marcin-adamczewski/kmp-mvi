package com.adamczewski.kmpmvi.mvi.settings

import kotlin.reflect.KClass

fun interface MviSettingsProvider {
    fun provide(tag: String, klass: KClass<out Any>): MviSettings
}
