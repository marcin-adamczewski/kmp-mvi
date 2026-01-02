package com.adamczewski.kmpmvi.mvi.utils

import com.adamczewski.kmpmvi.mvi.MviConfig
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.MviSettings

public fun MviComponent<out MviAction, out MviState, out MviEffect>.defaultSettings(): MviSettings {
    val klass = this::class
    val tag = "${klass.simpleName}@${this.hashCode().toHexString()}"
    return MviConfig.settingsProvider.provide(tag, klass)
}
