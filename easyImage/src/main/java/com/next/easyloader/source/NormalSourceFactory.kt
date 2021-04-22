package com.next.easyloader.source

import okhttp3.OkHttpClient

class NormalSourceFactory(private val okHttp: OkHttpClient) : SourceFactory {
    override fun create(url: String): Source {
        return NormalSource(url, okHttp)
    }
}
