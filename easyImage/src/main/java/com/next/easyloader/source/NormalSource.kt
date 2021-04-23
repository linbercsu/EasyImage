package com.next.easyloader.source

import okhttp3.OkHttpClient
import java.io.File
import java.net.URI

class NormalSource(private val url: String, okHttp: OkHttpClient) : Source() {
    private lateinit var source: Source

    init {
        val uri = URI.create(url)
        when (uri.scheme) {
            "file" -> source = FileSource(File(uri))
            "http", "https" -> source = HttpSource(url, okHttp)
        }
    }

    override fun getBytes(): ByteArray {
        return source.getBytes()
    }

    override fun getCacheKey(): String {
        return source.getCacheKey()
    }
}