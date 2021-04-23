package com.next.easyloader.memorycache

import android.graphics.drawable.Drawable

interface MemoryCache {
    fun put(key: String, drawable: Drawable)
    fun get(key: String): Drawable?
}