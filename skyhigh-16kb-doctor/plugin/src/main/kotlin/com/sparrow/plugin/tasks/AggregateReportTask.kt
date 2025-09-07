package com.sparrow.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter

abstract class AggregateReportTask : DefaultTask() {

    @get:Input
    abstract val failOnViolation: Property<Boolean>

    @get:Input
    abstract val perAbiFailList: ListProperty<String>

    @get:OutputDirectory
    abstract val finalDir: DirectoryProperty

    @get:InputDirectory
    abstract val scanDir: DirectoryProperty

    @get:InputFile
    abstract val ownersFile: RegularFileProperty

    @TaskAction
    fun aggregate() {
        val dir = finalDir.get().asFile.apply { mkdirs() }
        val scanDirectory = scanDir.get().asFile
        val owners = ownersFile.get().asFile
        val finalCsv = File(dir, "final.csv")
        val finalMd = File(dir, "final.md")
        val findings = mutableListOf<Map<String, String>>()

        val jsonFiles = scanDirectory.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
        jsonFiles.forEach { jf ->
            val j = jf.readText()
            val map = parseFlatJson(j)
            findings.add(map)
        }

         fun parseJsonArray(json: String): List<Map<String, String>> {
            val objectRegex = Regex("\\{[^}]*\\}")
            return objectRegex.findAll(json).map { matchResult ->
                parseFlatJson(matchResult.value)
            }.toList()
        }


        val ownersMap = mutableMapOf<String, String>()
        if (owners.exists()) {
            val ownersList = parseJsonArray(owners.readText())
            ownersList.forEach { obj ->
                val filePath = obj["filePath"] ?: "unknown"
                val owner = obj["ownerCoordinate"] ?: return@forEach
                val abi = extractAbi(filePath)
                val soName = filePath.substringAfterLast('/')
                val ownerKey = "$abi|$soName"
                ownersMap[ownerKey] = owner
            }
        }

        FileWriter(finalCsv).use { w ->
            w.append("artifact,path,abi,p_align,compatible,owner,remediation\n")
            findings.forEach { f ->
                val path = f["path"] ?: "unknown"
                val abi = f["abi"] ?: extractAbi(path)
                val soName = path.substringAfterLast('/')
                val ownerKey = "$abi|$soName"
                val owner = ownersMap[ownerKey] ?: "unknown"
                val pAlign = f["p_align"] ?: "0"
                val compatible = f["compatible"] ?: "false"
                val remediation = if (compatible.toBoolean()) "" else remediationHint(owner)
                w.append("${f["artifact"]},$path,${f["abi"]},$pAlign,$compatible,$owner,\"$remediation\"\n")
            }
        }

        FileWriter(finalMd).use { w ->
            w.append("# SkyHigh 16KB Doctor Report\n\n")
            w.append("| artifact | path | abi | p_align | 16kb compatible | owner | remediation |\n")
            w.append("|---|---|---:|---:|---:|---|---|\n")
            findings.forEach { f ->
                val path = f["path"] ?: "unknown"
                val abi = f["abi"] ?: extractAbi(path)
                val soName = path.substringAfterLast('/')
                val ownerKey = "$abi|$soName"
                val owner = ownersMap[ownerKey] ?: "unknown"
                val pAlign = f["p_align"] ?: "0"
                val compatible = f["compatible"] ?: "false"
                val remediation = if (compatible.toBoolean()) "" else remediationHint(owner)
                w.append("| ${f["artifact"]} | $path | ${f["abi"]} | $pAlign | $compatible | $owner | $remediation |\n")
            }
        }

        FileWriter(File(dir, "final.html")).use { w ->
            w.append("""
        <html>
        <head>
            <title>SkyHigh 16KB Doctor Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 2em; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                th { background: #f4f4f4; }
                tr:nth-child(even) { background: #fafafa; }
            </style>
        </head>
        <body>
            <h1>SkyHigh 16KB Doctor Report</h1>
            <table>
                <tr>
                    <th>Artifact</th>
                    <th>Path</th>
                    <th>ABI</th>
                    <th>p_align</th>
                    <th>16kb Compatible</th>
                    <th>Owner</th>
                    <th>Remediation</th>
                </tr>
    """.trimIndent())
            findings.forEach { f ->
                val path = f["path"] ?: "unknown"
                val abi = f["abi"] ?: extractAbi(path)
                val soName = path.substringAfterLast('/')
                val ownerKey = "$abi|$soName"
                val owner = ownersMap[ownerKey] ?: "unknown"
                val pAlign = f["p_align"] ?: "0"
                val compatible = f["compatible"] ?: "false"
                val remediation = if (compatible.toBoolean()) "" else remediationHint(owner)
                w.append("""
            <tr>
                <td>${f["artifact"]}</td>
                <td>$path</td>
                <td>$abi</td>
                <td>$pAlign</td>
                <td>$compatible</td>
                <td>$owner</td>
                <td>$remediation</td>
            </tr>
        """.trimIndent())
            }
            w.append("""
            </table>
        </body>
        </html>
    """.trimIndent())
        }


        val htmlReport = File(dir, "final.html")
        logger.lifecycle("Final HTML report generated at: ${htmlReport.absolutePath}")

        val os = System.getProperty("os.name").lowercase()
        try {
            when {
                os.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", htmlReport.absolutePath))
                }
                os.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", htmlReport.absolutePath))
                }
                os.contains("nux") || os.contains("nix") -> {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", htmlReport.absolutePath))
                }
                else -> {
                    logger.lifecycle("Automatic opening not supported on this OS.")
                }
            }
            logger.lifecycle("Opened HTML report in default browser.")
        } catch (e: Exception) {
            logger.warn("Could not open HTML report automatically: ${e.message}")
        }


        if (failOnViolation.get()) {
            val violations = findings.filter { it["compatible"]?.toBoolean() == false && !perAbiFailList.get().contains(it["abi"]) }
            if (violations.isNotEmpty()) {
                throw RuntimeException("Found ${violations.size} non-compliant native libraries (failOnViolation=true). See ${finalCsv.absolutePath}")
            }
        }
    }

    private fun parseFlatJson(j: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val rx = Regex("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(true|false)|([0-9]+))")
        rx.findAll(j).forEach { m ->
            val k = m.groupValues[1]
            val v = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            map[k] = v
        }
        return map
    }

    private fun extractAbi(path: String): String {
        val regex = Regex("(armeabi-v7a|arm64-v8a|x86_64|x86|mips|mips64)")
        return regex.find(path)?.value ?: "unknown"
    }


    private fun remediationHint(owner: String): String {
        return when {
            owner.startsWith(":") -> "Contact module owner $owner — rebuild native lib with 16KB p_align or set linker flags to align sections."
            owner.contains(":") -> "Open issue with dependency $owner and request 16KB p_align aligned builds."
            else -> "Unknown owner. Rebuild native library with increased p_align or contact binary vendor."
        }
    }
}