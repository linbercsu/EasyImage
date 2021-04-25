package com.next.easyloader.diskcache

import android.content.Context
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.util.*
import java.util.zip.CRC32

internal class LruDiskCache(private val context: Context) : DiskCache {
    companion object {
        private const val MAX_SIZE = 250 * 1024 * 1024
    }

    private val map: LinkedHashMap<String, DiskEntry> = LinkedHashMap()
    private val maxSize = MAX_SIZE
    private var size: Int = 0
    private lateinit var dir: File
    private var initialized = false
    private lateinit var journal: File
    private lateinit var buffer: BufferedSink

    private fun initialize() {
        synchronized(this) {
            if (initialized)
                return

            initialized = true

            var root = context.externalCacheDir
            if (root == null || !root.exists()) {
                root = context.cacheDir
            }

            dir = File(root, "easy-l")
            dir.mkdirs()
            journal = File(dir, "__journal")

            readJournal()

            flush()
        }
    }

    private fun readJournal() {
        try {
            val buffer = Okio.buffer(Okio.source(journal))
            buffer.use {
                while (true) {
                    if (buffer.exhausted())
                        break
                    val length = buffer.readInt()
                    if (length == -1)
                        break
                    val byteArray = buffer.readByteArray(length.toLong())
                    val path = String(byteArray)
                    val action = buffer.readByte().toInt()
                    val crc = buffer.readInt()
                    val crc32 = CRC32()
                    crc32.update(byteArray)
                    crc32.update(action)
                    if (crc32.value.toInt() == crc) {
                        val diskEntry = DiskEntry(path, action, crc)
                        if (action == ACTION_ADD) {
                            val file = File(path)
                            if (file.exists() && file.isFile) {
                                size += file.length().toInt()
                                map[path] = diskEntry
                            }

                        } else if (action == ACTION_REMOVE) {
                            map.remove(path)
                        }
                    }
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun put(key: String, data: ByteArray) {
        initialize()

        val file = File(dir, key)
        val tempFile = File(dir, "${key}.tmp")
        tempFile.writeBytes(data)
        if (tempFile.renameTo(file)) {
            synchronized(map) {
                val entry = DiskEntry(key, ACTION_ADD)
                map[key] = entry
                write(entry, true)

                trimToSize(maxSize)
            }
        }
    }

    override fun get(key: String): File? {
        initialize()

        synchronized(map) {
            val get = map[key] ?: return null
        }
        val file = File(dir, key)
        return if (file.exists() && file.isFile) {
            val entry = DiskEntry(key, ACTION_ADD)
            synchronized(map) {
            write(entry, true)
        }
            file
        } else
            null
    }

    private fun write(entry: DiskEntry, flush: Boolean) {
        val byteArray = entry.path.toByteArray()
        buffer.writeInt(byteArray.size)
        buffer.write(byteArray)
        buffer.writeByte(entry.action)
        buffer.writeInt(entry.crc)
        if (flush)
            buffer.flush()
    }

    private fun flush() {
        buffer = Okio.buffer(Okio.sink(journal))
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            write(next.value, false)
        }

        buffer.flush()
    }

    private fun trimToSize(maxSize: Int) {
        while (true) {
            if (size <= maxSize || map.isEmpty()) {
                break
            }

            val toEvict: Map.Entry<String, DiskEntry> = map.entries.iterator().next();
            val key = toEvict.key
            map.remove(key)
            val entry = DiskEntry(key, ACTION_REMOVE)
            write(entry, true)
            val file = File(key)
            if (file.exists() && file.isFile) {
                size -= file.length().toInt()
                file.delete()
            }

        }
    }
}

private const val ACTION_ADD = 0
private const val ACTION_REMOVE = 1

internal class DiskEntry(val path: String, val action: Int, crc: Int = 0) {

    val crc: Int
    init {
        if (crc != 0) {
            this.crc = crc
        } else {
            val crC32 = CRC32()
            val byteArray = path.toByteArray()
            crC32.update(byteArray)
            crC32.update(action)
            this.crc = crC32.value.toInt()
        }
    }
}