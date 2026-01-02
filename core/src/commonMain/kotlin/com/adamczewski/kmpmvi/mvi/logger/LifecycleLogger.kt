package com.adamczewski.kmpmvi.mvi.logger

public interface LifecycleLogger {
    public fun onInit()
    public fun onSubscribe()
    public fun onUnsubscribe()
    public fun onClear()
}
