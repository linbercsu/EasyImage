package com.next.easyloader.source

abstract class Source {
    abstract fun getBytes(): ByteArray
    abstract fun getCacheKey(): String
    open val type: String = ""
}