package com.next.easyloader.source

import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException

class HttpSource(private val url: String, private val client: OkHttpClient) : Source() {

    override fun getBytes(): ByteArray {

        val httpUrl = HttpUrl.get(url)

        val builder = Request.Builder()
        builder.url(httpUrl)

        val request = builder.build()

        val call: Call = client.newCall(request)

        val res = call.execute()
        val statusCode = res.code()

        if (statusCode == 200) {
            res.body().use { body ->
                if (body != null) {
                    return body.bytes()
                }
            }
        } else {
            res.body().use { }
        }

        throw FileNotFoundException()
    }


    override fun getCacheKey(): String {
        return url
    }

    override fun equals(other: Any?): Boolean {
        if (other !is HttpSource)
            return false

        return url == other.url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

}