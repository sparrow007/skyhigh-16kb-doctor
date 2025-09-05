package com.sparrow.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Task that triggers assembly of the specified Android variant.
 * This is a coordination task that depends on the actual Android assemble task.
 */
abstract class AssembleVariantTask : DefaultTask() {

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val assemble: Property<Boolean>

    @TaskAction
    fun assemble() {
        if (!assemble.get()) {
            logger.lifecycle("SkyHigh Doctor: Skipping assemble (disabled in configuration)")
            return
        }

        val variantName = variant.get()
        logger.lifecycle("SkyHigh Doctor: Assemble task for variant '$variantName' completed")
    }
}