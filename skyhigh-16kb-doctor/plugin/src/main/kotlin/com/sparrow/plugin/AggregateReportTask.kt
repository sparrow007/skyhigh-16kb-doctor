package com.sparrow.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter

open class AggregateReportTask : DefaultTask() {
    @get:Nested
    val extension = project.objects.property(DoctorExtension::class.java)

    @get:OutputDirectory
    val finalDir = project.layout.buildDirectory.dir("skyhigh/reports/final")

    @TaskAction
    fun aggregate() {
        val ext = extension.get()
        val dir = finalDir.get().asFile.apply { mkdirs() }
        val scanDir = File(project.buildDir, "skyhigh/reports/scan")
        val ownersFile = File(project.buildDir, "skyhigh/reports/owners/owners.csv")
        val finalCsv = File(dir, "final.csv")
        val finalMd = File(dir, "final.md")
        val findings = mutableListOf<Map<String, String>>()

        // read all per-entry JSON files (worker writes them)
        val jsonFiles = scanDir.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
        jsonFiles.forEach { jf ->
            val j = jf.readText() // each JSON is a single object
            // Very light-weight parsing: assume simple flat JSON - parse heuristically
            val map = parseFlatJson(j)
            findings.add(map)
        }

        // load owners map
        val owners = mutableMapOf<String, String>()
        if (ownersFile.exists()) {
            ownersFile.readLines().drop(1).forEach { line ->
                val parts = line.split(",", limit = 4)
                if (parts.size >= 4) {
                    val path = parts[2]
                    val owner = parts[3]
                    owners[path] = owner
                }
            }
        }

        FileWriter(finalCsv).use { w ->
            w.append("artifact,path,abi,p_align,compatible,owner,remediation\n")
            findings.forEach { f ->
                val path = f["path"] ?: "unknown"
                val owner = owners[path] ?: "unknown"
                val pAlign = f["p_align"] ?: "0"
                val compatible = f["compatible"] ?: "false"
                val remediation = if (compatible.toBoolean()) "" else remediationHint(owner, path)
                w.append("${f["artifact"]},$path,${f["abi"]},$pAlign,$compatible,$owner,\"$remediation\"\n")
            }
        }

        FileWriter(finalMd).use { w ->
            w.append("# SkyHigh 16KB Doctor Report\n\n")
            w.append("| artifact | path | abi | p_align | compatible | owner | remediation |\n")
            w.append("|---|---|---:|---:|---:|---|---|\n")
            findings.forEach { f ->
                val path = f["path"] ?: "unknown"
                val owner = owners[path] ?: "unknown"
                val pAlign = f["p_align"] ?: "0"
                val compatible = f["compatible"] ?: "false"
                val remediation = if (compatible.toBoolean()) "" else remediationHint(owner, path)
                w.append("| ${f["artifact"]} | $path | ${f["abi"]} | $pAlign | $compatible | $owner | $remediation |\n")
            }
        }

        logger.lifecycle("Final report generated at: ${dir.absolutePath}")

        if (ext.failOnViolation.get()) {
            val violations = findings.filter { it["compatible"]?.toBoolean() == false && !ext.perAbiFailList.get().contains(it["abi"]) }
            if (violations.isNotEmpty()) {
                throw RuntimeException("Found ${violations.size} non-compliant native libraries (failOnViolation=true). See ${finalCsv.absolutePath}")
            }
        }
    }

    private fun parseFlatJson(j: String): Map<String, String> {
        // naive simple parser for the small JSON objects we produce
        val map = mutableMapOf<String, String>()
        val rx = Regex("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+)|true|false|null)")
        rx.findAll(j).forEach { m ->
            val k = m.groupValues[1]
            val v = m.groupValues[2].ifEmpty { m.groupValues[3] }
            map[k] = v
        }
        return map
    }

    private fun remediationHint(owner: String, path: String): String {
        return when {
            owner.startsWith(":") -> "Contact module owner $owner — rebuild native lib with 16KB p_align or set linker flags to align sections."
            owner.contains(":") -> "Open issue with dependency $owner and request 16KB p_align aligned builds."
            else -> "Unknown owner. Rebuild native library with increased p_align or contact binary vendor."
        }
    }
}