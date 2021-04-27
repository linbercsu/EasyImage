package com.next.easyloader.internal

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import com.next.easyloader.memorycache.CacheSizeProvider

internal class DrawableHelper {
    companion object {
        fun calculateDrawableSize(drawable: Drawable): Int {
            if (drawable is CacheSizeProvider) {
                return drawable.objectSize()
            } else {
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    return bitmap.byteCount
                } else if (drawable is RoundedBitmapDrawable) {
                    val bitmap = drawable.bitmap
                    if (bitmap != null) {
                        return bitmap.byteCount
                    }
                }
            }

            return 1024//1k
        }
    }
}