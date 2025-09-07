package com.sparrow.plugin

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension DSL for SkyHigh Doctor
 */
abstract class DoctorExtension @Inject constructor(project: Project) {

    val variant: Property<String> = project.objects.property(String::class.java)
        .convention("debug")

    val assemble: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    val scanApk: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    val scanBundle: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)

    val reportDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("skyhigh/reports"))

    val maxAlign: Property<Long> = project.objects.property(Long::class.java)
        .convention(16384L)

    val failOnViolation: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)

    val perAbiFailList: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(emptyList())

    val ownersYaml: RegularFileProperty = project.objects.fileProperty()

    val parallelism: Property<Int> = project.objects.property(Int::class.java)
        .convention(4)

    val cacheDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("skyhigh/cache"))
}
