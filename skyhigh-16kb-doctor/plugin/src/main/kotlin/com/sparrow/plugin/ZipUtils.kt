package com.sparrow.plugin


import java.io.File
import java.util.zip.ZipFile

/**
 * Utility functions for working with ZIP files (APK/AAB).
 */
object ZipUtils {

    /**
     * Execute a block of code with a ZipFile, automatically closing it afterwards.
     */
    inline fun <T> withZipFile(file: File, block: (ZipFile) -> T): T {
        return ZipFile(file).use(block)
    }

    /**
     * Check if a ZIP entry is compressed.
     */
    fun isCompressed(entry: java.util.zip.ZipEntry): Boolean {
        return entry.method != java.util.zip.ZipEntry.STORED
    }
}