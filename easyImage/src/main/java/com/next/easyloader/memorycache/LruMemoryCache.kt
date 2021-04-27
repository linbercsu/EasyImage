package com.next.easyloader.memorycache

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.next.easyloader.internal.DrawableHelper

internal class LruMemoryCache(private val maxSize: Int) :
    MemoryCache, BitmapPool {
    private val cache: LruCache<String, Drawable> = object : LruCache<String, Drawable>(maxSize) {
        override fun sizeOf(key: String, value: Drawable): Int {
            return DrawableHelper.calculateDrawableSize(value)
        }
    }

    private val bitmapMap: LinkedHashMap<String, Bitmap> = LinkedHashMap()

    @Synchronized
    override fun put(key: String, drawable: Drawable) {
        drawable.setVisible(false, true)
        synchronized(cache) {
            cache.put(key, drawable)

            if (drawable is BitmapDrawable) {
                bitmapMap[key] = drawable.bitmap
            }
        }
    }

    @Synchronized
    override fun get(key: String): Drawable? {
        synchronized(cache) {
            val remove = cache.remove(key)
            if (remove != null) {
                bitmapMap.remove(key)
            }
            return remove
        }
    }

    override fun getBitmap(w: Int, h: Int, config: Bitmap.Config): Bitmap? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (config == Bitmap.Config.HARDWARE)
                return null
        }

        synchronized(cache) {
            val iterator = bitmapMap.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val value = next.value
                val width = value.width
                val height = value.height
                val config1 = value.config

                if (width == w && height == h && config1 == config) {
                    iterator.remove()
                    cache.remove(next.key)
                    return value
                }
            }
        }

        return null
    }
}