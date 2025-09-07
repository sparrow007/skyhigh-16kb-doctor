package com.sparrow.plugin.elf

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lightweight ELF reader to extract program header p_align fields.
 *
 * Supports ELF32/ELF64 and both endiannesses.
 */
object ElfReader {
    private const val EI_CLASS = 4
    private const val EI_DATA = 5
    private const val ELF_MAGIC0: Byte = 0x7F
    private const val ELF_MAGIC1: Byte = 0x45 // 'E'
    private const val ELF_MAGIC2: Byte = 0x4C // 'L'
    private const val ELF_MAGIC3: Byte = 0x46 // 'F'

    data class ElfInfo(val is64: Boolean, val order: ByteOrder, val phOff: Long, val phEntSize: Int, val phNum: Int)

    /**
     * Parse the ELF buffer and return the maximum p_align across program headers.
     * Returns 0 if no p_align found.
     */
    fun maxPAlign(buf: ByteArray): Long {
        try {
            val elf = parseHeader(buf) ?: return 0L
            var maxAlign = 0L
            var offset = elf.phOff
            for (i in 0 until elf.phNum) {
                if (offset + elf.phEntSize > buf.size) break
                val entryOffset = offset.toInt()
                val align = if (elf.is64) {
                    // p_align at entryOffset + 48 (u64)
                    val off = entryOffset + 48
                    if (off + 8 <= buf.size) {
                        readU64(buf, off, elf.order)
                    } else 0L
                } else {
                    // p_align at entryOffset + 28 (u32)
                    val off = entryOffset + 28
                    if (off + 4 <= buf.size) {
                        readU32(buf, off, elf.order).toLong()
                    } else 0L
                }
                if (align > maxAlign) maxAlign = align
                offset += elf.phEntSize
            }
            return maxAlign
        } catch (t: Throwable) {
            // Graceful: log and return 0 to mean "unknown"
            return 0L
        }
    }

    fun parseHeader(buf: ByteArray): ElfInfo? {
        if (buf.size < 16) return null
        if (buf[0] != ELF_MAGIC0 || buf[1] != ELF_MAGIC1 || buf[2] != ELF_MAGIC2 || buf[3] != ELF_MAGIC3) {
            return null
        }
        val cls = buf[EI_CLASS].toInt()
        val data = buf[EI_DATA].toInt()
        val order = when (data) {
            1 -> ByteOrder.LITTLE_ENDIAN
            2 -> ByteOrder.BIG_ENDIAN
            else -> ByteOrder.nativeOrder()
        }
        val is64 = (cls == 2)
        return if (is64) {
            if (buf.size < 64) return null
            val e_phoff = readU64(buf, 32, order)
            val e_phentsize = readU16(buf, 54, order)
            val e_phnum = readU16(buf, 56, order)
            ElfInfo(true, order, e_phoff, e_phentsize, e_phnum)
        } else {
            if (buf.size < 52) return null
            val e_phoff = readU32(buf, 28, order).toLong()
            val e_phentsize = readU16(buf, 42, order)
            val e_phnum = readU16(buf, 44, order)
            ElfInfo(false, order, e_phoff, e_phentsize, e_phnum)
        }
    }

    private fun readU16(buf: ByteArray, offset: Int, order: java.nio.ByteOrder): Int {
        val bb = ByteBuffer.wrap(buf, offset, 2).order(order)
        return bb.short.toInt() and 0xffff
    }

    private fun readU32(buf: ByteArray, offset: Int, order: java.nio.ByteOrder): Int {
        val bb = ByteBuffer.wrap(buf, offset, 4).order(order)
        return bb.int
    }

    private fun readU64(buf: ByteArray, offset: Int, order: java.nio.ByteOrder): Long {
        val bb = ByteBuffer.wrap(buf, offset, 8).order(order)
        return bb.long
    }
}
