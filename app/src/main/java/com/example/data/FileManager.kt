package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager(private val context: Context) {

    val workspaceDir: File
        get() = File(context.filesDir, "workspace").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    suspend fun initializeWorkspace(): List<PythonFile> = withContext(Dispatchers.IO) {
        val dir = workspaceDir
        SampleScripts.SAMPLES.forEach { (filename, content) ->
            val file = File(dir, filename)
            if (!file.exists()) {
                file.writeText(content)
            } else {
                val currentText = file.readText()
                if (currentText.contains("\\\"\\\"\\\"") || currentText.contains("\\\"") || !currentText.contains("Robustes Modbus-Timing")) {
                    val merged = if (filename == "deye_12k_inverter.py") mergeUserConfig(currentText, content) else content
                    file.writeText(merged)
                }
            }
        }
        getFilesInternal()
    }

    private fun mergeUserConfig(oldScript: String, newTemplate: String): String {
        var result = newTemplate
        val ipMatch = Regex("""INVERTER_IP\s*=\s*["']([^"']+)["']""").find(oldScript)
        if (ipMatch != null) {
            val userIp = ipMatch.groupValues[1]
            result = result.replace(Regex("""INVERTER_IP\s*=\s*["'][^"']+["']"""), """INVERTER_IP = "$userIp"""")
        }
        val portMatch = Regex("""INVERTER_PORT\s*=\s*(\d+)""").find(oldScript)
        if (portMatch != null) {
            val userPort = portMatch.groupValues[1]
            result = result.replace(Regex("""INVERTER_PORT\s*=\s*\d+"""), """INVERTER_PORT = $userPort""")
        }
        val snMatch = Regex("""LOGGER_SERIAL\s*=\s*(\d+)""").find(oldScript)
        if (snMatch != null) {
            val userSn = snMatch.groupValues[1]
            if (userSn != "0") {
                result = result.replace(Regex("""LOGGER_SERIAL\s*=\s*\d+"""), """LOGGER_SERIAL = $userSn""")
            }
        }
        return result
    }

    suspend fun getFiles(): List<PythonFile> = withContext(Dispatchers.IO) {
        getFilesInternal()
    }

    private fun getFilesInternal(): List<PythonFile> {
        val files = workspaceDir.listFiles { f -> f.isFile && (f.name.endsWith(".py") || f.name.endsWith(".json") || f.name.endsWith(".html") || f.name.endsWith(".txt")) } ?: emptyArray()
        return files.sortedBy { it.name.lowercase() }.map { file ->
            PythonFile(
                name = file.name,
                file = file,
                lastModified = file.lastModified(),
                sizeBytes = file.length()
            )
        }
    }

    suspend fun createFile(name: String, initialContent: String = ""): PythonFile = withContext(Dispatchers.IO) {
        val sanitizedName = if (name.endsWith(".py")) name else "$name.py"
        val file = File(workspaceDir, sanitizedName)
        if (!file.exists()) {
            file.writeText(initialContent)
        }
        PythonFile(
            name = file.name,
            file = file,
            lastModified = file.lastModified(),
            sizeBytes = file.length()
        )
    }

    suspend fun readFile(file: PythonFile): String = withContext(Dispatchers.IO) {
        if (file.file.exists()) {
            val text = file.file.readText()
            var repaired = text
            if (repaired.contains("\\\"\\\"\\\"") || repaired.contains("\\\"<!DOCTYPE")) {
                repaired = repaired.replace("\\\"\\\"\\\"", "\"\"\"").replace("\\\"", "\"")
            }
            if (file.name == "deye_12k_inverter.py" && !repaired.contains("Robustes Modbus-Timing")) {
                repaired = mergeUserConfig(text, SampleScripts.DEYE_SCRIPT)
            }
            if (repaired != text) {
                file.file.writeText(repaired)
            }
            repaired
        } else {
            ""
        }
    }

    suspend fun saveFile(file: PythonFile, content: String): PythonFile = withContext(Dispatchers.IO) {
        file.file.writeText(content)
        PythonFile(
            name = file.file.name,
            file = file.file,
            lastModified = file.file.lastModified(),
            sizeBytes = file.file.length()
        )
    }

    suspend fun renameFile(file: PythonFile, newName: String): PythonFile = withContext(Dispatchers.IO) {
        val sanitized = if (newName.contains(".")) newName else "$newName.py"
        val target = File(workspaceDir, sanitized)
        if (file.file.exists() && file.file != target) {
            file.file.renameTo(target)
        }
        PythonFile(
            name = target.name,
            file = target,
            lastModified = target.lastModified(),
            sizeBytes = target.length()
        )
    }

    suspend fun deleteFile(file: PythonFile): Boolean = withContext(Dispatchers.IO) {
        if (file.file.exists()) {
            file.file.delete()
        } else {
            false
        }
    }

    suspend fun resetAllSamples(): List<PythonFile> = withContext(Dispatchers.IO) {
        SampleScripts.SAMPLES.forEach { (filename, content) ->
            val file = File(workspaceDir, filename)
            file.writeText(content)
        }
        getFilesInternal()
    }
}
