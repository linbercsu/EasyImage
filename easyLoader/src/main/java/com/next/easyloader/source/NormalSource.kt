package com.next.easyloader.source

import okhttp3.OkHttpClient
import java.io.File
import java.net.URI

class NormalSource(private val url: String, private val okHttp: OkHttpClient) : Source() {
    private lateinit var source: Source
    override val type: String
        get() {
            val uri = URI.create(url)
            val file = File(uri.path)
            return file.extension
        }

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