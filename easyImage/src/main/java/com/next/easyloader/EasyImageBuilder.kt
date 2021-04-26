package com.next.easyloader

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ExecutorService

class EasyImageBuilder(
    internal val application: Application,
    internal val io: ExecutorService? = null,
    internal val main: CoroutineDispatcher? = null,
    internal val memoryCacheSize: Int = 0
) {
    fun build() {
        EasyImage.install(this)
    }
}