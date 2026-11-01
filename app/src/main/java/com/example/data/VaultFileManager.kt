package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FileCategory(val label: String) {
    ALL("الكل"),
    IMAGE("الصور"),
    VIDEO("الفيديو"),
    AUDIO("الصوتيات"),
    DOCUMENT("المستندات والعروض");

    companion object {
        fun fromMimeOrExtension(mime: String?, name: String): FileCategory {
            val lowerMime = (mime ?: "").lowercase()
            val lowerName = name.lowercase()

            return when {
                lowerMime.startsWith("image/") || lowerName.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif|bmp|svg)$")) -> IMAGE
                lowerMime.startsWith("video/") || lowerName.matches(Regex(".*\\.(mp4|mkv|mov|avi|webm|3gp)$")) -> VIDEO
                lowerMime.startsWith("audio/") || lowerName.matches(Regex(".*\\.(mp3|wav|m4a|aac|flac|ogg)$")) -> AUDIO
                lowerMime.contains("presentation") || lowerMime.contains("powerpoint") ||
                        lowerMime.contains("pdf") || lowerMime.contains("word") ||
                        lowerMime.contains("document") || lowerMime.contains("sheet") ||
                        lowerMime.startsWith("text/") ||
                        lowerName.matches(Regex(".*\\.(ppt|pptx|pdf|doc|docx|xls|xlsx|txt|rtf|csv)$")) -> DOCUMENT
                else -> DOCUMENT
            }
        }
    }
}

data class VaultFileItem(
    val id: String,
    val fileName: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val category: FileCategory,
    val absolutePath: String
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val file: File
        get() = File(absolutePath)
}

data class VaultStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val imageCount: Int,
    val videoCount: Int,
    val audioCount: Int,
    val documentCount: Int
) {
    val formattedTotalSize: String
        get() {
            val kb = totalSizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$totalSizeBytes B"
            }
        }
}

class VaultFileManager(private val context: Context) {

    private val vaultDirectory: File by lazy {
        val dir = File(context.filesDir, "vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val indexFile: File by lazy {
        File(context.filesDir, "vault_index.json")
    }

    companion object {
        private const val TAG = "VaultFileManager"
    }

    @Synchronized
    fun getVaultFiles(categoryFilter: FileCategory = FileCategory.ALL, query: String = ""): List<VaultFileItem> {
        val allItems = loadMetadataIndex()
        // Filter out any entries whose physical file was removed
        val validItems = allItems.filter { it.file.exists() }
        if (validItems.size != allItems.size) {
            saveMetadataIndex(validItems)
        }

        return validItems.filter { item ->
            val matchesCategory = (categoryFilter == FileCategory.ALL || item.category == categoryFilter)
            val matchesQuery = query.isBlank() || item.originalName.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun importFiles(uris: List<Uri>): List<VaultFileItem> {
        val imported = mutableListOf<VaultFileItem>()
        val existing = loadMetadataIndex().toMutableList()

        for (uri in uris) {
            try {
                val originalName = queryFileName(uri) ?: "vault_file_${System.currentTimeMillis()}"
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val category = FileCategory.fromMimeOrExtension(mimeType, originalName)
                val id = UUID.randomUUID().toString()

                // Preserve file extension
                val extension = originalName.substringAfterLast('.', "")
                val internalFileName = if (extension.isNotBlank()) "$id.$extension" else id
                val destinationFile = File(vaultDirectory, internalFileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (destinationFile.exists()) {
                    val item = VaultFileItem(
                        id = id,
                        fileName = internalFileName,
                        originalName = originalName,
                        mimeType = mimeType,
                        sizeBytes = destinationFile.length(),
                        timestamp = System.currentTimeMillis(),
                        category = category,
                        absolutePath = destinationFile.absolutePath
                    )
                    existing.add(item)
                    imported.add(item)
                    Log.d(TAG, "Imported file to vault: $originalName (${item.formattedSize})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import uri: $uri", e)
            }
        }

        if (imported.isNotEmpty()) {
            saveMetadataIndex(existing)
        }
        return imported
    }

    @Synchronized
    fun deleteFile(item: VaultFileItem): Boolean {
        try {
            if (item.file.exists()) {
                item.file.delete()
            }
            val existing = loadMetadataIndex().toMutableList()
            existing.removeAll { it.id == item.id }
            saveMetadataIndex(existing)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete vault file: ${item.originalName}", e)
            return false
        }
    }

    fun getShareableUri(item: VaultFileItem): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create shareable Uri", e)
            null
        }
    }

    fun exportToUri(item: VaultFileItem, targetUri: Uri): Boolean {
        return try {
            item.file.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    input.copyTo(output)
                    true
                } ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export file to $targetUri", e)
            false
        }
    }

    fun getStats(): VaultStats {
        val files = getVaultFiles(FileCategory.ALL)
        return VaultStats(
            totalFiles = files.size,
            totalSizeBytes = files.sumOf { it.sizeBytes },
            imageCount = files.count { it.category == FileCategory.IMAGE },
            videoCount = files.count { it.category == FileCategory.VIDEO },
            audioCount = files.count { it.category == FileCategory.AUDIO },
            documentCount = files.count { it.category == FileCategory.DOCUMENT }
        )
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query display name from uri", e)
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun loadMetadataIndex(): List<VaultFileItem> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val content = indexFile.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<VaultFileItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val catString = obj.optString("category", FileCategory.DOCUMENT.name)
                val category = try {
                    FileCategory.valueOf(catString)
                } catch (e: Exception) {
                    FileCategory.DOCUMENT
                }
                list.add(
                    VaultFileItem(
                        id = obj.getString("id"),
                        fileName = obj.getString("fileName"),
                        originalName = obj.getString("originalName"),
                        mimeType = obj.getString("mimeType"),
                        sizeBytes = obj.getLong("sizeBytes"),
                        timestamp = obj.getLong("timestamp"),
                        category = category,
                        absolutePath = obj.getString("absolutePath")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vault index", e)
            emptyList()
        }
    }

    private fun saveMetadataIndex(items: List<VaultFileItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("fileName", item.fileName)
                    put("originalName", item.originalName)
                    put("mimeType", item.mimeType)
                    put("sizeBytes", item.sizeBytes)
                    put("timestamp", item.timestamp)
                    put("category", item.category.name)
                    put("absolutePath", item.absolutePath)
                }
                jsonArray.put(obj)
            }
            indexFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save vault index", e)
        }
    }
}
