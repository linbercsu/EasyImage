package com.next.easyloader.decoder

import android.graphics.drawable.Drawable

interface Decoder {
    fun decode(bytes: ByteArray, w: Int, h: Int): Drawable?
}