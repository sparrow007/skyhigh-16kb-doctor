package com.sparrow.plugin

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ZipUtils {
    /**
     * List paths of entries ending with .so under lib/ or jni/ directories.
     */
    fun listSoEntries(zip: File): List<String> {
        val list = mutableListOf<String>()
        if (!zip.exists()) return list
        ZipFile(zip).use { zf ->
            zf.entries().asSequence().forEach { e ->
                if (!e.isDirectory && e.name.endsWith(".so")) {
                    // include entries under lib/ or jni/ or anywhere .so found
                    list.add(e.name)
                }
            }
        }
        return list
    }

    /**
     * Read entry bytes for a given entry path.
     */
    fun readEntryBytes(zip: File, entryPath: String): ByteArray {
        ZipFile(zip).use { zf ->
            val e = zf.getEntry(entryPath) ?: throw IllegalArgumentException("Entry $entryPath not found in ${zip.name}")
            val isCompressed = e.method != ZipEntry.STORED
            val bytes = zf.getInputStream(e).readBytes()
            // If compressed, we still return the bytes (decompressed) but caller should be warned about compression.
            return bytes
        }
    }
}
