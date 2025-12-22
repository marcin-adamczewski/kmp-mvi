package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.MviSettings

internal fun StateComponent<out MviAction, out MviState, out MviEffect>.defaultSettings(): MviSettings {
    val klass = this::class
    val tag = "${klass.simpleName}@${this.hashCode().toHexString()}"
    return MviConfig.settingsProvider.provide(tag, klass)
}
