package com.sparrow.plugin


import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Entry point for the Gradle plugin.
 */
class SkyHighDoctorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("skyhighDoctor", DoctorExtension::class.java, project.objects)

        // create tasks
        val assembleTask = project.tasks.register(
            "skyhighAssemble",
            AssembleVariantTask::class.java
        ) {
            group = "verification"
            description = "Assemble Android variant (if configured)"
        }


        val scanTask = project.tasks.register("skyhighScan", ScanOutputsTask::class.java) {
            group = "verification"
            description = "Scan APK/AAB outputs for native .so libraries"
            this.extension.set(extension)
            mustRunAfter(assembleTask)
        }

        val ownersTask = project.tasks.register("skyhighMapOwners", MapOwnersTask::class.java) {
            group = "verification"
            description = "Map .so files to module or dependency owners"
            this.extension.set(extension)
        }

        val reportTask = project.tasks.register("skyhighReport", AggregateReportTask::class.java) {
            group = "verification"
            description = "Aggregate findings and owners into final reports"
            this.extension.set(extension)
            mustRunAfter(scanTask, ownersTask)
        }

        // top-level orchestration task
        val orchestrate = project.tasks.register("skyhighDoctor") {
            group = "verification"
            description = "Run the full SkyHigh 16KB doctor pipeline"
            dependsOn(assembleTask, scanTask, ownersTask, reportTask)
        }

        project.afterEvaluate {
            project.logger.lifecycle("SkyHighDoctor configured. Variant=${extension.variant.get()} assemble=${extension.assemble.get()}")
        }
    }
}
