package com.next.easyloader

import android.app.Application

class App : Application() {

    override fun onCreate() {
        EasyImage.install(this)
        super.onCreate()
    }
}