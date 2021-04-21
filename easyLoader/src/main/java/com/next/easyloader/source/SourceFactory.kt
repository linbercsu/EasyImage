package com.next.easyloader.source

interface SourceFactory {
    fun create(url: String): Source
}