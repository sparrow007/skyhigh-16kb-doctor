package com.sparrow.plugin

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension DSL for SkyHigh Doctor
 */
abstract class DoctorExtension @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {

    val variant: Property<String> = objects.property(String::class.java).convention("debug")
    val assemble: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val scanApk: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val scanBundle: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val reportDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.buildDirectory.dir("skyhigh/reports"))

    val maxAlign: Property<Long> = objects.property(Long::class.java).convention(16384L)
    val failOnViolation: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val perAbiFailList: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())

    val ownersYaml: RegularFileProperty = objects.fileProperty()

    val parallelism: Property<Int> = objects.property(Int::class.java).convention(4)

    val cacheDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.buildDirectory.dir("skyhigh/cache"))
}

