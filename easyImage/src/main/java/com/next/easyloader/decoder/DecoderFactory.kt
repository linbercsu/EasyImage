package com.next.easyloader.decoder

import com.next.easyloader.decoder.Decoder

interface DecoderFactory {
    fun create(): Decoder
}