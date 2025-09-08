package com.sparrow.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter
import java.util.zip.ZipFile

abstract class ScanNativeSoTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jniLibsDirs: ListProperty<File>

    @get:Input
    abstract val aarArtifacts: ListProperty<File>

    @get:Input
    abstract val targetSoFiles: ListProperty<String>

    @get:OutputFile
    val reportJson = project.layout.buildDirectory.file("skyhigh/reports/owners/owners.json")

    @TaskAction
    fun scan() {
        val outFile = reportJson.get().asFile.apply { parentFile.mkdirs() }
        val entries = mutableListOf<String>()


        // Scan local jniLibs
        jniLibsDirs.get().forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { so ->
                val relative = so.relativeTo(dir).path
                entries.add(
                    """
  {
    "sourceType":"module",
    "moduleOrDependency":"$dir",
    "filePath":"$relative",
    "ownerCoordinate":"$dir"
  }
""".trimIndent()
                )

            }
        }

        // Scan AAR artifacts for .so files
        aarArtifacts.get().forEach { aar ->
            if (aar.isFile && aar.extension == "aar") {
                val zip = ZipFile(aar)
                val matchingSoEntries = zip.entries().asSequence()
                    .filter { it.name.endsWith(".so") }
                    .filter { entry ->
                        targetSoFiles.get().isEmpty() ||
                                targetSoFiles.get().any { soName -> entry.name.endsWith(soName) }
                    }
                    .toList()
                matchingSoEntries.forEach { entry ->
                    entries.add(
                        """
  {
    "sourceType":"dependency",
    "moduleOrDependency":"${aar.name}",
    "filePath":"${entry.name}",
    "ownerCoordinate":"${aar.name}"
  }
""".trimIndent()
                    )

                }
            }
        }

        val jsonArray = "[\n${entries.joinToString(",\n")}\n]"

        FileWriter(outFile).use { it.write(jsonArray) }

        logger.lifecycle("In ScanNative Owners JSON generated: ${outFile.absolutePath}")
    }
}