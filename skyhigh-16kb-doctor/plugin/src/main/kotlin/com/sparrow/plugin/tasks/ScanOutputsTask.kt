package com.sparrow.plugin.tasks

import com.sparrow.plugin.util.ZipUtils
import com.sparrow.plugin.worker.SoScanWorkAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class ScanOutputsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val objectFactor: ObjectFactory
) : DefaultTask() {

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val scanApk: Property<Boolean>

    @get:Input
    abstract val scanBundle: Property<Boolean>

    @get:InputDirectory
    @get:Optional
    abstract val apkDir: DirectoryProperty

    @get:InputDirectory
    @get:Optional
    abstract val bundleDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun scan() {
        val outDir = outputDir.get().asFile.apply { mkdirs() }

        val candidates = mutableListOf<File>()

        if (scanApk.get()) {
            if (apkDir.isPresent) {
                val dir = apkDir.get().asFile
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.extension == "apk" }?.let { candidates.addAll(it) }
                }
            }
        }
        if (scanBundle.get()) {
            if (bundleDir.isPresent) {
                val dir = bundleDir.get().asFile
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.extension in listOf("aab", "zip") }?.let { candidates.addAll(it) }
                }
            }
        }

        if (candidates.isEmpty()) {
            logger.warn("No APK/AAB candidates found under build/outputs for variant='$variant'.")
        }

        val svc = workerExecutor.noIsolation()

        candidates.forEach { logger.lifecycle("  - ${it.name} (lastModified=${it.lastModified()})") }

        candidates.sortedBy { it.lastModified() }
            .forEach { file ->
                val entries = ZipUtils.listSoEntries(file)
                if (entries.isEmpty()) {
                    logger.lifecycle("No .so entries found in ${file.name}")
                }
                entries.forEach { entry ->
                    val bytes = ZipUtils.readEntryBytes(file, entry)
                    // Submit to worker for parsing ELF
                    svc.submit(SoScanWorkAction::class.java) {
                        input.set(objectFactor.fileProperty().fileValue(file))
                        entryPath.set(entry)
                        this.bytes.set(bytes)
                        maxAlign.set(16384L)
                        reportDir.set(outDir)
                    }
                }
            }

        // We rely on Worker API to produce per-entry json output; wait for workers to finish by not exiting task
        // Worker API ensures actions have completed when task ends
        logger.lifecycle("Submitted ${candidates.size} candidate artifacts for scanning. Reports will be in: ${outDir.absolutePath}")
    }
}