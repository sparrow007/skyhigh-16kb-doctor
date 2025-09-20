package com.sparrow.plugin


import com.sparrow.plugin.tasks.AggregateReportTask
import com.sparrow.plugin.tasks.ScanNativeSoTask
import com.sparrow.plugin.tasks.ScanOutputsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Entry point for the Gradle plugin.
 */
class SkyHighDoctorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("skyhighDoctor", DoctorExtension::class.java, project.objects)

        val scanTask = project.tasks.register("skyhighScan", ScanOutputsTask::class.java) {
            group = "SkyHigh 16KB Doctor"
            description = "Scan APK/AAB outputs for native .so libraries"
            this.scanApk.convention(extension.scanApk.get())
            this.scanBundle.convention(extension.scanBundle.get())
            this.variant.convention(extension.variant.get())
            this.apkDirPath.set("${project.projectDir}/build/outputs/apk/${extension.variant.get()}")
            this.bundleDirPath.set("${project.projectDir}/build/outputs/bundle/${extension.variant.get()}")
            this.outputDir.set(project.layout.buildDirectory.dir("skyhigh/reports/scan"))
            dependsOn("assemble")
        }


        val ownersTask = project.tasks.register("skyhighScanNativeSo", ScanNativeSoTask::class.java) {
            group = "SkyHigh 16KB Doctor"
            description = "Scan all modules and dependencies for native .so libraries"

            val allProjects = project.rootProject.allprojects
            val jniDirs = allProjects.map { File(it.projectDir, "src/main/jniLibs") }.filter { it.exists() }
            val aarFiles = mutableListOf<File>()

            allProjects.forEach { subProj ->
                listOf("debugCompileClasspath", "releaseCompileClasspath").forEach { configName ->
                    val config = subProj.configurations.findByName(configName)
                    if (config != null && config.isCanBeResolved) {
                        logger.lifecycle("  ⚙️  Checking configuration: $configName")

                        val artifacts = config.incoming.artifactView {
                            isLenient = true
                        }.artifacts.artifacts
                        aarFiles.addAll(artifacts.map { it.file }.filter { it.extension == "aar" })
                    }
                }
            }

            this.jniLibsDirs.set(jniDirs)
            this.aarArtifacts.set(aarFiles)
            this.targetSoFiles.set(listOf())
            mustRunAfter(scanTask)// or set specific .so names if needed
        }

        val reportTask = project.tasks.register("skyhighReport", AggregateReportTask::class.java) {
            group = "SkyHigh 16KB Doctor"
            description = "Aggregate findings and owners into final reports"
            this.failOnViolation.convention(extension.failOnViolation.getOrElse(false))
            this.perAbiFailList.set(extension.perAbiFailList.getOrElse(emptyList()))
            this.finalDir.set(project.layout.buildDirectory.dir("skyhigh/reports/final"))
            this.scanDir.set(project.layout.buildDirectory.dir("skyhigh/reports/scan"))
            this.ownersFile.set(project.layout.buildDirectory.file("skyhigh/reports/owners/owners.json"))
            mustRunAfter(scanTask, ownersTask)
        }

        project.tasks.register("skyhighDoctor") {
            group = "SkyHigh 16KB Doctor"
            description = "Run the full SkyHigh 16KB doctor pipeline"
            dependsOn(scanTask, ownersTask, reportTask)
        }
    }
}
