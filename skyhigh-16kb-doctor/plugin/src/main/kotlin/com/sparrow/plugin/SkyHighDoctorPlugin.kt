package com.sparrow.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * SkyHigh 16KB Page Alignment Doctor Plugin
 *
 * This plugin scans Android APK/AAB outputs for native libraries that are not
 * compatible with 16KB page sizes, maps them to their owners, and generates
 * comprehensive reports.
 */
class SkyHighDoctorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create("skyhighDoctor", DoctorExtension::class.java)

        // Configure default values
        extension.variant.convention("debug")
        extension.assemble.convention(true)
        extension.scanApk.convention(true)
        extension.scanBundle.convention(false)
        extension.reportDir.convention(project.layout.buildDirectory.dir("skyhigh/reports"))
        extension.maxAlign.convention(16384L)
        extension.failOnViolation.convention(false)
        extension.parallelism.convention(4)
        extension.cacheDir.convention(project.layout.buildDirectory.dir("skyhigh/cache"))



        // Register tasks
        val assembleTask = project.tasks.register("skyhighAssemble", AssembleVariantTask::class.java) { task ->
            task.group = "verification"
            task.description = "Assembles the specified variant for scanning"
            task.variant.set(extension.variant)
            task.assemble.set(extension.assemble)
        }

        val scanTask = project.tasks.register("skyhighScan", ScanOutputsTask::class.java) { task ->
            task.group = "verification"
            task.description = "Scans APK/AAB outputs for 16KB page alignment compatibility"
            task.variant.set(extension.variant)
            task.scanApk.set(extension.scanApk)
            task.scanBundle.set(extension.scanBundle)
            task.maxAlign.set(extension.maxAlign)
            task.parallelism.set(extension.parallelism)
            task.reportDir.set(extension.reportDir)
            task.cacheDir.set(extension.cacheDir)
        }

        val mapOwnersTask = project.tasks.register("skyhighMapOwners", MapOwnersTask::class.java) { task ->
            task.group = "verification"
            task.description = "Maps native libraries to their owners (modules or dependencies)"
            task.reportDir.set(extension.reportDir)
            task.ownersYaml.set(extension.ownersYaml)
        }

        val reportTask = project.tasks.register("skyhighReport", AggregateReportTask::class.java) { task ->
            task.group = "verification"
            task.description = "Generates final aggregate report with ownership information"
            task.reportDir.set(extension.reportDir)
            task.failOnViolation.set(extension.failOnViolation)
            task.perAbiFailList.set(extension.perAbiFailList)

            task.dependsOn(scanTask, mapOwnersTask)
        }

        val doctorTask = project.tasks.register("skyhighDoctor") { task ->
            task.group = "verification"
            task.description = "Runs complete 16KB page alignment analysis"
            task.dependsOn(assembleTask, reportTask)
        }

        // Wire up assemble task dependencies after project evaluation
        project.afterEvaluate {
            configureAssembleTaskDependencies(project, assembleTask.get(), extension)
        }
    }

    private fun configureAssembleTaskDependencies(
        project: Project,
        assembleTask: AssembleVariantTask,
        extension: DoctorExtension
    ) {
        if (!extension.assemble.get()) {
            return
        }

        val variant = extension.variant.get()
        val assembleTaskName = "assemble${variant.replaceFirstChar { it.uppercase() }}"

        // Try to find Android assemble task
        val androidAssembleTask = project.tasks.findByName(assembleTaskName)
        if (androidAssembleTask != null) {
            assembleTask.dependsOn(androidAssembleTask)
            project.logger.lifecycle("SkyHigh Doctor: Found Android assemble task '$assembleTaskName'")
        } else {
            project.logger.warn("SkyHigh Doctor: Android assemble task '$assembleTaskName' not found. " +
                    "Make sure the Android plugin is applied or disable assemble in extension.")
        }
    }
}
