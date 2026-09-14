package com.example.data

import java.io.File

data class PythonFile(
    val name: String,
    val file: File,
    val lastModified: Long = file.lastModified(),
    val sizeBytes: Long = if (file.exists()) file.length() else 0L
) {
    val path: String get() = file.absolutePath
    val isSample: Boolean get() = name.startsWith("demo_") || name.startsWith("sample_")
    
    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024f)
                else -> String.format("%.1f MB", sizeBytes / (1024f * 1024f))
            }
        }
}
