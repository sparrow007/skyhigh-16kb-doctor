package com.sparrow.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileWriter


@CacheableTask
open class MapOwnersTask : DefaultTask() {
    @get:Nested
    val extension = project.objects.property(DoctorExtension::class.java)

    @get:OutputFile
    val ownersCsv = project.layout.buildDirectory.file("skyhigh/reports/owners/owners.csv")

    @TaskAction
    fun mapOwners() {
        val ext = extension.get()
        val outFile = ownersCsv.get().asFile.apply {
            parentFile.mkdirs()
        }
        FileWriter(outFile).use { w ->
            w.append("sourceType,moduleOrDependency,filePath,ownerCoordinate\n")
            // 1) module-local jniLibs
            project.rootProject.allprojects.forEach { p ->
                val jniDir = File(p.projectDir, "src/main/jniLibs")
                if (jniDir.exists()) {
                    jniDir.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { so ->
                        val relative = so.relativeTo(p.projectDir).path
                        w.append("module,${p.path},${relative},${p.path}\n")
                    }
                }
            }

            // 2) resolved runtime artifacts: try to resolve runtimeClasspath for the root project or this project
            val configNameCandidates = listOf("runtimeClasspath", "runtime")
            val processed = mutableSetOf<File>()
            configNameCandidates.forEach { cfgName ->
                val cfg = project.configurations.findByName(cfgName)
                if (cfg != null) {
                    try {
                        cfg.resolvedConfiguration.resolvedArtifacts.forEach { ra ->
                            val f = ra.file
                            if (processed.add(f)) {
                                if (f.isFile && (f.extension == "aar" || f.extension == "zip" || f.extension == "jar")) {
                                    val soEntries = ZipUtils.listSoEntries(f)
                                    soEntries.forEach { entry ->
                                        w.append("dependency,${ra.moduleVersion.id},${entry},${ra.moduleVersion.id}\n")
                                    }
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        logger.warn("Failed to resolve configuration $cfgName: ${t.message}")
                    }
                }
            }
        }

        logger.lifecycle("Owners CSV generated: ${outFile.absolutePath}")
    }
}
