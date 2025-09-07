package com.sparrow.plugin


import com.sparrow.plugin.worker.SoScanWorkAction
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

@CacheableTask
open class ScanOutputsTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @get:Nested
    val extension = project.objects.property(DoctorExtension::class.java)

    @get:OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("skyhigh/reports/scan")

    @TaskAction
    fun scan() {
        val ext = extension.get()
        val outDir = outputDir.get().asFile.apply { mkdirs() }
        val findings = ConcurrentLinkedQueue<Map<String, Any>>()

        val candidates = mutableListOf<File>()
        val variant = ext.variant.get()

        if (ext.scanApk.get()) {
            val apkDir = File(project.projectDir, "build/outputs/apk/${variant}")
            if (apkDir.exists()) {
                apkDir.listFiles()?.filter { it.extension in listOf("apk") }?.let { candidates.addAll(it) }
            }
        }
        if (ext.scanBundle.get()) {
            val bundleDir = File(project.projectDir, "build/outputs/bundle/${variant}")
            if (bundleDir.exists()) {
                bundleDir.listFiles()?.filter { it.extension in listOf("aab","zip") }?.let { candidates.addAll(it) }
            }
        }

        if (candidates.isEmpty()) {
            logger.warn("No APK/AAB candidates found under build/outputs for variant='$variant'.")
        }

        val parallelism = ext.parallelism.get().coerceAtLeast(1)
        val svc = workerExecutor.noIsolation()

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
                        input.set(project.objects.fileProperty().fileValue(file))
                        entryPath.set(entry)
                        this.bytes.set(bytes)
                        maxAlign.set(ext.maxAlign.get())
                        reportDir.set(outDir)
                    }
                }
            }

        // We rely on Worker API to produce per-entry json output; wait for workers to finish by not exiting task
        // Worker API ensures actions have completed when task ends
        logger.lifecycle("Submitted ${candidates.size} candidate artifacts for scanning. Reports will be in: ${outDir.absolutePath}")
    }
}
