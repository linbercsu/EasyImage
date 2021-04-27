package com.next.easyloader.memorycache

import android.graphics.Bitmap

interface BitmapPool {
    fun getBitmap(w: Int, h: Int, config: Bitmap.Config): Bitmap?
}