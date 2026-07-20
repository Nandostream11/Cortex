package com.cortex.app

import android.app.Application
import com.cortex.app.di.AppContainer

class CortexApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
