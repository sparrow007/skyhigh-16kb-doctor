package com.sparrow.plugin


import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import kotlin.getOrElse
import kotlin.text.get
import kotlin.text.set

/**
 * Entry point for the Gradle plugin.
 */
class SkyHighDoctorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("skyhighDoctor", DoctorExtension::class.java, project.objects)

        val scanTask = project.tasks.register("skyhighScan", ScanOutputsTask::class.java) {
            notCompatibleWithConfigurationCache("Uses project properties at execution time")
            group = "verification"
            description = "Scan APK/AAB outputs for native .so libraries"
            this.scanApk.convention(extension.scanApk.get())
            this.scanBundle.convention(extension.scanBundle.get())
            this.variant.convention(extension.variant.get())
            this.apkDir.set(
                File(
                    project.projectDir,
                    "build/outputs/apk/${extension.variant.get()}"
                )
            )
            this.bundleDir.set(File(project.projectDir, "build/outputs/bundle/${extension.variant.get()}"))
            dependsOn("assemble")
        }


        val ownersTask = project.tasks.register("skyhighScanNativeSo", ScanNativeSoTask::class.java) {
            group = "verification"
            description = "Scan all modules and dependencies for native .so libraries"

            val allProjects = project.rootProject.allprojects
            val jniDirs = allProjects.map { File(it.projectDir, "src/main/jniLibs") }.filter { it.exists() }
            val aarFiles = mutableListOf<File>()

            allProjects.forEach { subProj ->
                listOf("debugCompileClasspath", "releaseCompileClasspath").forEach { configName ->
                    val config = subProj.configurations.findByName(configName)
                    if (config != null && config.isCanBeResolved) {
                        println("  ⚙️  Checking configuration: $configName")

                        val artifacts = config.incoming.artifactView {
                            isLenient = true
                        }.artifacts.artifacts

                        println("    📦 Found ${artifacts.size} total artifacts in $configName:")

                        aarFiles.addAll(artifacts.map { it.file }.filter { it.extension == "aar" })

                        //print each aar files
                        artifacts.forEach { artifact ->
                            val file = artifact.file
                            if (file.extension == "aar") {
                                println("      - ${file.name}")
                            }
                        }
                    }
                }
            }

            this.jniLibsDirs.set(jniDirs)
            this.aarArtifacts.set(aarFiles)
            this.targetSoFiles.set(listOf())
            mustRunAfter(scanTask)// or set specific .so names if needed
        }


        val reportTask = project.tasks.register("skyhighReport", AggregateReportTask::class.java) {
            group = "verification"
            description = "Aggregate findings and owners into final reports"
            this.failOnViolation.convention(extension.failOnViolation.getOrElse(false))
            this.perAbiFailList.set(extension.perAbiFailList.getOrElse(emptyList()))
            this.finalDir.set(project.layout.buildDirectory.dir("skyhigh/reports/final"))
            this.scanDir.set(project.layout.buildDirectory.dir("skyhigh/reports/scan"))
            this.ownersFile.set(project.layout.buildDirectory.file("skyhigh/reports/owners/owners.json"))
            mustRunAfter(scanTask, ownersTask)
        }

//
//        val reportTask = project.tasks.register("skyhighReport", AggregateReportTask::class.java) {
//            group = "verification"
//            description = "Aggregate findings and owners into final reports"
//            this.extension.set(extension)
//            mustRunAfter(scanTask, ownersTask)
//        }
//
//        // top-level orchestration task
//        val orchestrate = project.tasks.register("skyhighDoctor") {
//            group = "verification"
//            description = "Run the full SkyHigh 16KB doctor pipeline"
//            dependsOn(assembleTask, scanTask, ownersTask, reportTask)
//        }
//
//        project.afterEvaluate {
//            project.logger.lifecycle("SkyHighDoctor configured. Variant=${extension.variant.get()} assemble=${extension.assemble.get()}")
//        }

        project.tasks.register("skyhighDoctor") {
            group = "verification"
            description = "Run the full SkyHigh 16KB doctor pipeline"
            dependsOn(scanTask, ownersTask, reportTask)
        }
    }
}
