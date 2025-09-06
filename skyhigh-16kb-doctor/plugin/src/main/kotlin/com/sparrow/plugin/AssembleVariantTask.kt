package com.sparrow.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task that triggers assembly of the specified Android variant
 * and locates the output APK.
 * This is a coordination task that depends on the actual Android assemble task.
 */
abstract class AssembleVariantTask : DefaultTask() {

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val assemble: Property<Boolean>

    @get:OutputFile
    abstract val apkPath: RegularFileProperty // This will hold the path to the APK

    @TaskAction
    fun assemble() {
        if (!assemble.get().also { shouldAssemble ->
            if (!shouldAssemble) {
                logger.lifecycle("SkyHigh Doctor: Skipping assemble (disabled in configuration)")
            }
        }) {
            return
        }

        val variantName = variant.get()
        logger.lifecycle("SkyHigh Doctor: Locating APK for variant '$variantName' after assembly...")

        // The actual Android assemble task (e.g., :app:assembleDebug) should have run before this.
        // This task's job is to find that APK.

        val outputApkDirectoryPath = "outputs/apk/$variantName"
        val outputApkDirectory = project.layout.buildDirectory.get().asFile.resolve(outputApkDirectoryPath)
        var foundApkFile: File? = null

        if (outputApkDirectory.exists() && outputApkDirectory.isDirectory) {
            val apkFiles = outputApkDirectory.listFiles { file -> file.isFile && file.name.endsWith(".apk") }

            if (apkFiles != null && apkFiles.isNotEmpty()) {
                // Try to find an APK that matches common naming conventions.
                val preferredApkName1 = "${project.name}-${variantName}.apk"
                val preferredApkName2 = "app-${variantName}.apk" // Common for the main app module

                foundApkFile = apkFiles.find { it.name.equals(preferredApkName1, ignoreCase = true) }
                if (foundApkFile == null) {
                    foundApkFile = apkFiles.find { it.name.equals(preferredApkName2, ignoreCase = true) }
                }
                if (foundApkFile == null) {
                    if (apkFiles.size == 1) {
                        foundApkFile = apkFiles[0]
                    } else {
                        // Attempt to find a 'universal' apk or one without ABI qualifiers if multiple exist
                        foundApkFile = apkFiles.find { apk ->
                            !apk.name.contains("-armeabi-v7a-") &&
                            !apk.name.contains("-arm64-v8a-") &&
                            !apk.name.contains("-x86-") &&
                            !apk.name.contains("-x86_64-") &&
                            (apk.name.endsWith("universal.apk") || apk.name.endsWith("$variantName.apk"))
                        } ?: apkFiles[0] // Fallback to the first one
                        logger.lifecycle("SkyHigh Doctor: Multiple APKs found in $outputApkDirectory. Selected: ${foundApkFile?.name}. All found: ${apkFiles.joinToString { it.name }}")
                    }
                }

                if (foundApkFile != null) {
                    logger.lifecycle("SkyHigh Doctor: Found APK: ${foundApkFile.absolutePath}")
                    apkPath.set(foundApkFile)
                } else {
                    logger.error("SkyHigh Doctor: Could not select an APK from the found files in $outputApkDirectory for variant '$variantName'. Files: ${apkFiles.joinToString { it.name }}")
                }
            } else {
                logger.error("SkyHigh Doctor: No APK files found in directory: $outputApkDirectory. Ensure the assemble task for '$variantName' ran successfully and produced an APK.")
            }
        } else {
            logger.error("SkyHigh Doctor: APK output directory does not exist or is not a directory: $outputApkDirectory. Ensure the assemble task for '$variantName' ran successfully.")
        }

        if (apkPath.isPresent) {
            logger.lifecycle("SkyHigh Doctor: APK path for variant '$variantName' is set to ${apkPath.get().asFile.absolutePath}")
        } else {
            throw IllegalStateException("SkyHigh Doctor: Failed to find APK for variant '$variantName'. Check logs for details. The assemble task might have failed or an APK was not found in the expected location: $outputApkDirectoryPath")
        }
        logger.lifecycle("SkyHigh Doctor: AssembleVariantTask for '$variantName' completed.")
    }
}
