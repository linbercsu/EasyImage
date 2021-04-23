package com.next.easyloader.gif

import com.next.easyloader.decoder.Decoder
import com.next.easyloader.decoder.DecoderFactory
import kotlinx.coroutines.CoroutineDispatcher

class GifDecoderFactory(private val io: CoroutineDispatcher, private val main: CoroutineDispatcher) :
    DecoderFactory {
    override fun create(): Decoder {
        return DefaultGifDecoder(io, main)
    }

}