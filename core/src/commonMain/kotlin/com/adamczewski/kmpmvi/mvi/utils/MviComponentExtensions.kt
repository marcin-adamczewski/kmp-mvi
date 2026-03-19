package com.adamczewski.kmpmvi.mvi.utils

import com.adamczewski.kmpmvi.mvi.MviConfig
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.mvi.settings.MviSettingsBuilder
import com.adamczewski.kmpmvi.mvi.settings.buildMviSettings

public fun <T: Any> T.defaultMviSettings(
    logTag: String? = null
): MviSettings {
    val klass = this::class
    val tag = logTag ?: "${klass.simpleName}@${this.hashCode().toHexString()}"
    return MviConfig.settingsProvider.provide(tag, klass)
}

public fun MviComponent<out MviAction, out MviState, out MviEffect, out MviMessage>.buildSettings(
    block: MviSettingsBuilder.() -> Unit
): MviSettings = buildMviSettings(defaultMviSettings(), block)
