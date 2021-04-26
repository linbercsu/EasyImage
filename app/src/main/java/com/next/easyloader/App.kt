package com.next.easyloader

import android.app.Application

class App : Application() {

    override fun onCreate() {
        EasyImageBuilder(this, memoryCacheSize = 1024 * 1024 * 100)
        EasyImage.install(this)
        super.onCreate()
    }
}