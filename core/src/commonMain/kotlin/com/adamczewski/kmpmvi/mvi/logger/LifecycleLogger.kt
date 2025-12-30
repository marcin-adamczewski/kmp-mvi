package com.adamczewski.kmpmvi.mvi.logger

interface LifecycleLogger {
    fun onInit()
    fun onSubscribe()
    fun onUnsubscribe()
    fun onClear()
}
