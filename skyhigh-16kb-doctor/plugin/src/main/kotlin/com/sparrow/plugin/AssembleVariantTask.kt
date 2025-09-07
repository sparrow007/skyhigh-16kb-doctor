package com.sparrow.plugin


import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.InputFiles
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.Project

open class AssembleVariantTask : DefaultTask() {
    @get:Nested
    val extension = project.objects.property(DoctorExtension::class.java)

    init {
        group = "verification"
        description = "Assemble variant (if assemble=true and Android plugin present)"
    }

    @TaskAction
    fun assemble() {
        val ext = extension.get()
        if (!ext.assemble.get()) {
            logger.lifecycle("Assemble disabled by configuration.")
            return
        }

        // Try to find assemble task names commonly used by AGP
        val variantCapitalized = ext.variant.get().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val candidateNames = listOf("assemble$variantCapitalized", "assemble${variantCapitalized}UnitTest", "assemble")
        val found = candidateNames.mapNotNull { name ->
            project.tasks.findByName(name)
        }.firstOrNull()

        if (found != null) {
            logger.lifecycle("Triggering assemble task: ${found.name}")
            project.tasks.named(found.name).get().actions.forEach { /* no-op: we will just depend */ }
            // add dependsOn to ensure order when run via CLI
            this.dependsOn(found)
        } else {
            logger.warn("No assemble task found for variant '${ext.variant.get()}'. If you want to scan artifacts, ensure APK/AAB outputs exist or set assemble=false.")
        }
    }
}
