package com.sparrow.plugin.worker

import com.sparrow.plugin.elf.ElfReader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.io.FileWriter

interface SoScanParameters : WorkParameters {
    val input: RegularFileProperty
    val entryPath: Property<String>
    val bytes: Property<ByteArray>
    val maxAlign: Property<Long>
    val reportDir: Property<File>
}

abstract class SoScanWorkAction : WorkAction<SoScanParameters> {
    override fun execute() {
        val params = parameters
        val artifact = params.input.asFile.get()
        val entry = params.entryPath.get()
        val bytes = params.bytes.get()
        val maxAlign = params.maxAlign.get()

        val pAlign = try {
            ElfReader.maxPAlign(bytes)
        } catch (_: Throwable) {
            0L
        }
        val compatible = pAlign >= maxAlign

        val json = buildJson(artifact.name, entry, detectAbiFromPath(entry), pAlign, compatible)
        // write file into reportDir
        val dir = params.reportDir.get()
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "${artifact.name}-${entry.replace("/", "_")}.json")
        FileWriter(out).use { it.write(json) }
    }

    private fun detectAbiFromPath(path: String): String {
        val parts = path.split("/")
        return parts.firstOrNull { it.matches(Regex("arm64-v8a|armeabi-v7a|x86|x86_64")) } ?: "unknown"
    }

    private fun buildJson(artifact: String, path: String, abi: String, pAlign: Long, compatible: Boolean): String {
        return """{
  "artifact":"$artifact",
  "path":"$path",
  "abi":"$abi",
  "p_align":$pAlign,
  "compatible":$compatible
}"""
    }
}
