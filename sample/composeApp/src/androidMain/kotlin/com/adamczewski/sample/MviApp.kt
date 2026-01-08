package com.adamczewski.sample

import android.app.Application
import com.adamczewski.kmpmvi.sample.di.initKoin

class MviApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
