package com.tutozz.blespam

import android.app.Application
import android.content.Context

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        App.instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}