package com.next.easyloader.internal

internal class TypeDetector {
    companion object {
        const val TYPE_GIF = "gif"
        const val TYPE_UNKNOWN = "unknown"

        fun detect(byteArray: ByteArray): String {
            return if (byteArray[0] == 'G'.toByte() && byteArray[1] == 'I'.toByte() && byteArray[2] == 'F'.toByte()) {
                TYPE_GIF
            } else {
                TYPE_UNKNOWN
            }

        }
    }
}