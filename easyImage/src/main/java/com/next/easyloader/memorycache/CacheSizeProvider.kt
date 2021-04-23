package com.next.easyloader.memorycache

interface CacheSizeProvider {
    fun objectSize(): Int
}