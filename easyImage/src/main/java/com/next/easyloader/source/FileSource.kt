package com.next.easyloader.source

import java.io.File

class FileSource(private val file: File) : Source() {

    override fun getBytes(): ByteArray {
        return file.readBytes()
    }

    override fun getCacheKey(): String {
        return file.canonicalPath
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileSource)
            return false

        return this.file == other.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}