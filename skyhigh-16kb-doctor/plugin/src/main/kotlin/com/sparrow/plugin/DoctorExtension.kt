package com.sparrow.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the SkyHigh Doctor plugin.
 */
abstract class DoctorExtension {

    /**
     * The Android build variant to analyze (e.g., "debug", "release").
     * Default: "debug"
     */
    abstract val variant: Property<String>

    /**
     * Whether to automatically run the assemble task for the variant.
     * Default: true
     */
    abstract val assemble: Property<Boolean>

    /**
     * Whether to scan APK files for analysis.
     * Default: true
     */
    abstract val scanApk: Property<Boolean>

    /**
     * Whether to scan Android App Bundle (AAB) files for analysis.
     * Default: false
     */
    abstract val scanBundle: Property<Boolean>

    /**
     * Directory where reports will be generated.
     * Default: project.layout.buildDirectory.dir("skyhigh/reports")
     */
    abstract val reportDir: DirectoryProperty

    /**
     * Maximum alignment value to consider compatible (in bytes).
     * Libraries with p_align >= this value are considered compatible.
     * Default: 16384 (16KB)
     */
    abstract val maxAlign: Property<Long>

    /**
     * Whether to fail the build when incompatible libraries are found.
     * Default: false
     */
    abstract val failOnViolation: Property<Boolean>

    /**
     * List of ABI names to exclude from failure checks.
     * Even if failOnViolation is true, violations in these ABIs won't fail the build.
     */
    abstract val perAbiFailList: ListProperty<String>

    /**
     * Optional YAML file mapping library names to owner information.
     * Format: libraryName -> { owner: "team", contact: "email" }
     */
    abstract val ownersYaml: RegularFileProperty

    /**
     * Number of parallel workers to use for scanning native libraries.
     * Default: 4
     */
    abstract val parallelism: Property<Int>

    /**
     * Directory for caching intermediate results.
     * Default: project.layout.buildDirectory.dir("skyhigh/cache")
     */
    abstract val cacheDir: DirectoryProperty
}