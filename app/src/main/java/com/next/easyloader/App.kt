package com.next.easyloader

import android.app.Application

class App : Application() {

    override fun onCreate() {
        EasyLoader.install(this)
        super.onCreate()
    }
}