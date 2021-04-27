package com.next.easyloader.memorycache

import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.next.easyloader.internal.DrawableHelper

internal class LruMemoryCache(private val maxSize: Int) :
    MemoryCache {
    private val cache: LruCache<String, Drawable> = object : LruCache<String, Drawable>(maxSize) {
        override fun sizeOf(key: String, value: Drawable): Int {
            return DrawableHelper.calculateDrawableSize(value)
        }
    }

    @Synchronized
    override fun put(key: String, drawable: Drawable) {
        drawable.setVisible(false, true)
        synchronized(cache) {
            cache.put(key, drawable)
        }
    }

    @Synchronized
    override fun get(key: String): Drawable? {
        synchronized(cache) {
            return cache.remove(key)
        }
    }
}