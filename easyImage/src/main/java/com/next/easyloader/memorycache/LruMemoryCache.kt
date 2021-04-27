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

    private val map: MutableMap<BitmapKey, MutableList<String>> = mutableMapOf()

    @Synchronized
    override fun put(key: String, drawable: Drawable) {
        drawable.setVisible(false, true)
        synchronized(cache) {
            cache.put(key, drawable)

            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val bitmapKey = BitmapKey(bitmap.width, bitmap.height, bitmap.config)
                var list = map[bitmapKey]
                if (list == null) {
                    list = mutableListOf()
                    map[bitmapKey] = list
                }

                list.add(key)
            }
        }
    }

    @Synchronized
    override fun get(key: String): Drawable? {
        synchronized(cache) {
            val remove = cache.remove(key)
            if (remove != null) {
                if (remove is BitmapDrawable) {
                    val bitmap = remove.bitmap
                    val bitmapKey = BitmapKey(bitmap.width, bitmap.height, bitmap.config)
                    val mutableList = map[bitmapKey]
                    if (mutableList != null) {
                        val iterator = mutableList.iterator()
                        while (iterator.hasNext()) {
                            val next = iterator.next()
                            if (next == key) {
                                iterator.remove()
                                break
                            }
                        }
                    }
                }
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
            val bitmapKey = BitmapKey(w, h, config)
            val mutableList = map[bitmapKey]
            if (mutableList.isNullOrEmpty())
                return null

            val first = mutableList.removeAt(0)
            val remove = cache.remove(first)
            if (remove is BitmapDrawable) {
                return remove.bitmap
            }
        }

        return null
    }

}

private data class BitmapKey(private val w: Int, private val h: Int, private val config: Bitmap.Config)