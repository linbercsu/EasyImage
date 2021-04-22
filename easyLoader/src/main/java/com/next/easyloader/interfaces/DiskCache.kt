package com.next.easyloader.interfaces

import java.io.File

interface DiskCache {
    fun put(key: String, data: ByteArray)
    fun get(key: String): File?
}