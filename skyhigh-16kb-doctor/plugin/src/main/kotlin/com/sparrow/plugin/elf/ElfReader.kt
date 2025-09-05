package com.sparrow.plugin.elf

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ELF file reader for extracting program header alignment information.
 *
 * Supports both 32-bit and 64-bit ELF files with little and big endian byte ordering.
 */
class ElfReader private constructor(
    private val buffer: ByteBuffer,
    private val elfClass: Int, // 1 = ELF32, 2 = ELF64
    private val elfData: Int   // 1 = little endian, 2 = big endian
) {

    companion object {
        private const val ELF_MAGIC = 0x7F454C46 // 0x7F + "ELF"
        private const val EI_CLASS = 4
        private const val EI_DATA = 5
        private const val ELFCLASS32 = 1
        private const val ELFCLASS64 = 2
        private const val ELFDATA2LSB = 1
        private const val ELFDATA2MSB = 2

        /**
         * Parse ELF file from byte array.
         * Returns null if the file is not a valid ELF or cannot be parsed.
         */
        fun parse(bytes: ByteArray): ElfReader? {
            if (bytes.size < 64) return null // Minimum ELF header size

            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN) // Start with little endian for reading header

            // Check ELF magic
            val magic = buffer.getInt(0)
            if (magic != ELF_MAGIC.toInt()) return null

            // Get class and data encoding
            val elfClass = buffer.get(EI_CLASS).toInt() and 0xFF
            val elfData = buffer.get(EI_DATA).toInt() and 0xFF

            if (elfClass !in 1..2 || elfData !in 1..2) return null

            // Set correct byte order
            buffer.order(if (elfData == ELFDATA2LSB) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

            return ElfReader(buffer, elfClass, elfData)
        }
    }

    /**
     * Get the maximum p_align value from all program headers.
     */
    fun maxPAlign(): Long {
        try {
            val programHeaders = getProgramHeaders()
            return programHeaders.maxOfOrNull { it.pAlign } ?: 0L
        } catch (e: Exception) {
            // Return 0 for any parsing errors
            return 0L
        }
    }

    /**
     * Get all program headers from the ELF file.
     */
    private fun getProgramHeaders(): List<ProgramHeader> {
        val headers = mutableListOf<ProgramHeader>()

        // Read ELF header fields
        val (phOff, phEntSize, phNum) = if (elfClass == ELFCLASS64) {
            // ELF64 offsets: e_phoff at 32, e_phentsize at 54, e_phnum at 56
            Triple(
                buffer.getLong(32),
                buffer.getShort(54).toInt() and 0xFFFF,
                buffer.getShort(56).toInt() and 0xFFFF
            )
        } else {
            // ELF32 offsets: e_phoff at 28, e_phentsize at 42, e_phnum at 44
            Triple(
                buffer.getInt(28).toLong() and 0xFFFFFFFFL,
                buffer.getShort(42).toInt() and 0xFFFF,
                buffer.getShort(44).toInt() and 0xFFFF
            )
        }

        // Read each program header
        for (i in 0 until phNum) {
            val offset = phOff + (i * phEntSize)
            if (offset + phEntSize > buffer.capacity()) break

            val pAlign = if (elfClass == ELFCLASS64) {
                // ELF64: p_align at offset 48 within program header (8 bytes)
                buffer.getLong((offset + 48).toInt())
            } else {
                // ELF32: p_align at offset 28 within program header (4 bytes)
                buffer.getInt((offset + 28).toInt()).toLong() and 0xFFFFFFFFL
            }

            headers.add(ProgramHeader(pAlign = pAlign))
        }

        return headers
    }

    /**
     * Represents a program header with alignment information.
     */
    private data class ProgramHeader(
        val pAlign: Long
    )
}