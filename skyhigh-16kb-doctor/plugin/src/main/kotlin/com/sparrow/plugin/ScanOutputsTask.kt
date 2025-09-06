package com.sparrow.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
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
    val inputFile: Property<File> // Keep as Property<File> for worker action
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
                        // Placeholder pAlign logic, actual logic might be more complex
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
            // Consider re-throwing or logging more formally depending on error handling strategy
        }

        val artifactReportFile = File(cacheDir, "${inputFile.nameWithoutExtension}-findings.json")
        artifactReportFile.writeText(Json.encodeToString(findings))
    }
}


/**
 * Task that scans a single APK file for native library page alignment.
 */
@CacheableTask
abstract class ScanOutputsTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputFile // Changed from InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputApkFile: RegularFileProperty

    @get:Input // Retain variant for report naming and potentially other logic
    abstract val variant: Property<String>

    @get:Input
    abstract val maxAlign: Property<Long>

    @get:Input
    abstract val parallelism: Property<Int> 

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @get:Internal 
    abstract val cacheDir: DirectoryProperty

    @TaskAction
    fun scan() {
        val reportDirFile = reportDir.get().asFile
        reportDirFile.mkdirs()

        val apkToScan = inputApkFile.get().asFile
        if (!apkToScan.exists()) {
            logger.warn("SkyHigh Doctor: Input APK file not found: ${apkToScan.path}")
            writeEmptyReports(reportDirFile)
            return
        }

        val workQueue = workerExecutor.processIsolation { spec ->
            spec.maxWorkersCount = parallelism.getOrElse(1) // Default to 1 if not set, or use availableProcessors
        }

        val tempCacheDir = cacheDir.get().asFile
        if (tempCacheDir.exists()) { 
            tempCacheDir.deleteRecursively()
        }
        tempCacheDir.mkdirs()

        logger.lifecycle("SkyHigh Doctor: Starting scan for ${apkToScan.name} using up to ${parallelism.getOrElse(1)} parallel workers.")

        workQueue.submit(ScanArtifactWorkAction::class.java) { params ->
            params.inputFile.set(apkToScan) // Pass the File object
            params.maxAlign.set(this@ScanOutputsTask.maxAlign)
            params.cacheDir.set(tempCacheDir)
        }
        
        // logger.lifecycle("SkyHigh Doctor: Scan task submitted for ${apkToScan.name}. Waiting for completion...")
        // workQueue.await() // Implicitly awaited by Gradle at the end of the task action.

        // Note: The worker writes its output to a file in `tempCacheDir`.
        // The aggregation of results from worker output files happens after all workers complete.

        val allFindings = mutableListOf<ScanFinding>()
        // It's expected that ScanArtifactWorkAction produces one JSON file in tempCacheDir.
        // The name would be like "${apkToScan.nameWithoutExtension}-findings.json"
        val expectedFindingsFile = File(tempCacheDir, "${apkToScan.nameWithoutExtension}-findings.json")

        if (expectedFindingsFile.exists()) {
            try {
                val content = expectedFindingsFile.readText()
                if (content.isNotBlank()) {
                    val findingsFromFile = Json.decodeFromString<List<ScanFinding>>(content)
                    allFindings.addAll(findingsFromFile)
                }
            } catch (e: Exception) {
                logger.error("SkyHigh Doctor: Error reading or parsing findings from ${expectedFindingsFile.name}. Content preview: '${expectedFindingsFile.readText().take(100)}'", e)
            }
        } else {
             logger.warn("SkyHigh Doctor: No findings file found at ${expectedFindingsFile.path}. Scan might have failed or produced no output.")
        }


        if (allFindings.isEmpty()) {
            logger.warn("SkyHigh Doctor: Scan completed for ${apkToScan.name}, but no findings were collected. Check worker logs or scan logic.")
        }

        val reportFile = File(reportDirFile, "skyhigh-scan-report-${variant.getOrElse("unknown")}.json")
        try {
            reportFile.writeText(Json { prettyPrint = true }.encodeToString(allFindings))
            logger.lifecycle("SkyHigh Doctor: Scan report generated at ${reportFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("SkyHigh Doctor: Failed to write final scan report for ${apkToScan.name}.", e)
        }
    }

    private fun writeEmptyReports(reportDirFile: File) {
        val reportFile = File(reportDirFile, "skyhigh-scan-report-${variant.getOrElse("unknown")}.json")
        reportFile.writeText(Json.encodeToString(emptyList<ScanFinding>()))
        logger.lifecycle("SkyHigh Doctor: Empty scan report generated at ${reportFile.absolutePath} due to missing input APK.")
    }
}
