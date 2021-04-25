package com.next.easyloader.gif

import android.graphics.drawable.Drawable
import android.util.Log
import com.next.easyloader.decoder.Decoder
import kotlinx.coroutines.CoroutineDispatcher

class DefaultGifDecoder(
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher
) : Decoder {
    private val gifDecoder: GifDecoder =
        GifDecoder()
    override fun decode(bytes: ByteArray, w: Int, h: Int): Drawable? {
        val read = gifDecoder.read(bytes)
        return if (read == 0)
            GifDrawable(gifDecoder, io, main)
        else
            null
    }

}