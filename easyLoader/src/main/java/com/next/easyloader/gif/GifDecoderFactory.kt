package com.next.easyloader.gif

import com.next.easyloader.Decoder
import com.next.easyloader.DecoderFactory
import kotlinx.coroutines.CoroutineDispatcher

class GifDecoderFactory(private val io: CoroutineDispatcher, private val main: CoroutineDispatcher) :
    DecoderFactory {
    override fun create(): Decoder {
        return DefaultGifDecoder(io, main)
    }

}