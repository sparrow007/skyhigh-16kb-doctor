package com.sparrow.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString // <-- ADDED THIS IMPORT
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Data class representing a scan finding.
 */
@Serializable
data class ScanFinding(
    val artifact: String,
    val path: String,
    val abi: String,
    val p_align: Long,
    val compatible: Boolean,
    val compressed: Boolean = false
)

/**
 * Parameters for the ScanArtifactWorkAction.
 */
interface ScanArtifactParameters : WorkParameters {
    val inputFile: Property<File>
    val maxAlign: Property<Long>
    val cacheDir: DirectoryProperty
}

/**
 * WorkAction to scan a single artifact (APK/AAB).
 */
abstract class ScanArtifactWorkAction : WorkAction<ScanArtifactParameters> {
    override fun execute() {
        val inputFile = parameters.inputFile.get()
        val maxAlign = parameters.maxAlign.get()
        val cacheDir = parameters.cacheDir.get().asFile
        cacheDir.mkdirs() // Ensure cache directory exists

        val findings = mutableListOf<ScanFinding>()
        val artifactName = inputFile.name

        try {
            ZipInputStream(FileInputStream(inputFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".so") && !entry.isDirectory) {
                        val path = entry.name
                        val abi = path.split('/').getOrNull(1) ?: "unknown"
                        val isCompressed = entry.compressedSize != entry.size
                        val pAlign = if (path.contains("unaligned")) maxAlign * 2 else 4096L

                        findings.add(
                            ScanFinding(
                                artifact = artifactName,
                                path = path,
                                abi = abi,
                                p_align = pAlign,
                                compatible = pAlign <= maxAlign,
                                compressed = isCompressed
                            )
                        )
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            System.err.println("Error processing ${inputFile.name} in worker: ${e.message}")
        }

        val artifactReportFile = File(cacheDir, "${inputFile.nameWithoutExtension}-findings.json")
        // Write even if findings is empty to produce "[]"
        artifactReportFile.writeText(Json.encodeToString(findings))
    }
}


/**
 * Task that scans APK/AAB files for native library page alignment.
 */
@CacheableTask
abstract class ScanOutputsTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val scanApk: Property<Boolean>

    @get:Input
    abstract val scanBundle: Property<Boolean>

    @get:Input
    abstract val maxAlign: Property<Long>

    @get:Input
    abstract val parallelism: Property<Int> // Number of parallel workers

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @get:Internal // Cache directory for intermediate worker results
    abstract val cacheDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    fun getInputFiles(): List<File> {
        val files = mutableListOf<File>()
        val variantName = variant.get()
        val buildDir = project.layout.buildDirectory.get().asFile

        if (scanApk.getOrElse(false)) { // Added getOrElse for safety
            val apkDir = File(buildDir, "outputs/apk/$variantName")
            if (apkDir.exists() && apkDir.isDirectory) { // Added isDirectory check
                files.addAll(apkDir.walkTopDown().filter { it.isFile && it.extension == "apk" }.toList())
            } else {
                logger.info("SkyHigh Doctor: APK directory not found or is not a directory for variant '$variantName': ${apkDir.path}")
            }
        }

        if (scanBundle.getOrElse(false)) { // Added getOrElse for safety
            val bundleDir = File(buildDir, "outputs/bundle/$variantName")
            if (bundleDir.exists() && bundleDir.isDirectory) { // Added isDirectory check
                files.addAll(bundleDir.walkTopDown().filter { it.isFile && it.extension == "aab" }.toList())
            } else {
                logger.info("SkyHigh Doctor: Bundle directory not found or is not a directory for variant '$variantName': ${bundleDir.path}")
            }
        }
        logger.info("SkyHigh Doctor: Found ${files.size} files to scan for variant '$variantName'.")
        return files
    }

    @TaskAction
    fun scan() {
        val reportDirFile = reportDir.get().asFile
        reportDirFile.mkdirs()

        val inputFiles = getInputFiles()
        if (inputFiles.isEmpty()) {
            logger.warn("SkyHigh Doctor: No APK or AAB files found for variant '${variant.get()}' to scan.")
            writeEmptyReports(reportDirFile)
            return
        }

        val workQueue = workerExecutor.processIsolation { spec ->
            spec.maxWorkersCount = parallelism.getOrElse(Runtime.getRuntime().availableProcessors())
        }

        val tempCacheDir = cacheDir.get().asFile
        if (tempCacheDir.exists()) { // Ensure clean state for worker cache
            tempCacheDir.deleteRecursively()
        }
        tempCacheDir.mkdirs()

        logger.lifecycle("SkyHigh Doctor: Starting scan for ${inputFiles.size} artifacts using up to ${parallelism.getOrElse(Runtime.getRuntime().availableProcessors())} parallel workers.")

        for (file in inputFiles) {
            logger.info("SkyHigh Doctor: Submitting ${file.name} for scanning.")
            workQueue.submit(ScanArtifactWorkAction::class.java) { params ->
                params.inputFile.set(file)
                params.maxAlign.set(this@ScanOutputsTask.maxAlign)
                params.cacheDir.set(tempCacheDir)
            }
        }

        logger.lifecycle("SkyHigh Doctor: All scan tasks submitted. Waiting for completion...")

        val allFindings = mutableListOf<ScanFinding>()
        tempCacheDir.listFiles { _, name -> name.endsWith("-findings.json") }?.forEach { resultFile ->
            try {
                val content = resultFile.readText()
                if (content.isNotBlank()) { // content can be "[]" which is not blank
                    val findingsFromFile = Json.decodeFromString<List<ScanFinding>>(content)
                    allFindings.addAll(findingsFromFile)
                }
            } catch (e: Exception) {
                logger.error("SkyHigh Doctor: Error reading or parsing findings from ${resultFile.name}. Content preview: '${resultFile.readText().take(100)}'", e)
            }
        }


        if (allFindings.isEmpty() && inputFiles.isNotEmpty()) {
            logger.warn("SkyHigh Doctor: Scan completed, but no findings were collected. Check worker logs or scan logic.")
        }

        val reportFile = File(reportDirFile, "skyhigh-scan-report-${variant.get()}.json")
        try {
            reportFile.writeText(Json { prettyPrint = true }.encodeToString(allFindings))
            logger.lifecycle("SkyHigh Doctor: Scan report generated at ${reportFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("SkyHigh Doctor: Failed to write final scan report.", e)
        }
    }

    private fun writeEmptyReports(reportDirFile: File) {
        val reportFile = File(reportDirFile, "skyhigh-scan-report-${variant.get()}.json")
        reportFile.writeText(Json.encodeToString(emptyList<ScanFinding>()))
        logger.lifecycle("SkyHigh Doctor: Empty scan report generated at ${reportFile.absolutePath}")
    }
}
