package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.dao.PdfDao
import com.example.data.model.PdfDocumentItem
import com.example.engine.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfRepository(
    private val context: Context,
    private val pdfDao: PdfDao
) {
    val allPdfs: Flow<List<PdfDocumentItem>> = pdfDao.getAllPdfs()

    fun getPdfById(id: Long): Flow<PdfDocumentItem?> = pdfDao.getPdfById(id)

    suspend fun getPdfByIdSync(id: Long): PdfDocumentItem? = pdfDao.getPdfByIdSync(id)

    /**
     * Directory where PDFs are permanently stored
     */
    fun getPdfsDirectory(): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create a unique destination file for a new PDF
     */
    fun createDestinationFile(suggestedName: String): File {
        val sanitized = suggestedName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
        var targetFile = File(getPdfsDirectory(), fileName)
        var counter = 1
        val baseName = fileName.removeSuffix(".pdf")
        while (targetFile.exists()) {
            targetFile = File(getPdfsDirectory(), "${baseName}_$counter.pdf")
            counter++
        }
        return targetFile
    }

    /**
     * Save generated PDF into repository and DB
     */
    suspend fun savePdfRecord(
        title: String,
        file: File,
        pageCount: Int,
        isEncrypted: Boolean,
        thumbnailFile: File? = null,
        passwordHint: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val item = PdfDocumentItem(
            title = title,
            filePath = file.absolutePath,
            fileSizeBytes = file.length(),
            pageCount = pageCount,
            createdAt = System.currentTimeMillis(),
            isEncrypted = isEncrypted,
            passwordHint = passwordHint,
            thumbnailPath = thumbnailFile?.absolutePath
        )
        pdfDao.insertPdf(item)
    }

    /**
     * Rename PDF file and DB record
     */
    suspend fun renamePdf(id: Long, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        val item = pdfDao.getPdfByIdSync(id) ?: return@withContext false
        val oldFile = File(item.filePath)
        val sanitized = newTitle.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val newFileName = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
        val newFile = File(oldFile.parentFile ?: getPdfsDirectory(), newFileName)

        if (oldFile.exists() && oldFile != newFile) {
            val renamed = oldFile.renameTo(newFile)
            if (renamed) {
                pdfDao.updatePdf(
                    item.copy(
                        title = newTitle,
                        filePath = newFile.absolutePath
                    )
                )
                return@withContext true
            }
        } else {
            pdfDao.renamePdf(id, newTitle)
            return@withContext true
        }
        false
    }

    /**
     * Delete PDF from disk and DB
     */
    suspend fun deletePdf(id: Long): Boolean = withContext(Dispatchers.IO) {
        val item = pdfDao.getPdfByIdSync(id) ?: return@withContext false
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            item.thumbnailPath?.let { thumbPath ->
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
            pdfDao.deletePdfById(id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Lock an existing PDF with password
     */
    suspend fun lockExistingPdf(id: Long, password: String): Boolean = withContext(Dispatchers.IO) {
        val item = pdfDao.getPdfByIdSync(id) ?: return@withContext false
        val currentFile = File(item.filePath)
        if (!currentFile.exists()) return@withContext false

        val tempLockedFile = File(context.cacheDir, "locked_${System.currentTimeMillis()}.pdf")
        val success = PdfEngine.lockPdf(currentFile, tempLockedFile, password)
        if (success && tempLockedFile.exists()) {
            currentFile.delete()
            tempLockedFile.renameTo(currentFile)
            pdfDao.updateEncryptionStatus(id, true)
            return@withContext true
        }
        false
    }

    /**
     * Unlock an existing password-protected PDF
     */
    suspend fun unlockExistingPdf(id: Long, password: String): Boolean = withContext(Dispatchers.IO) {
        val item = pdfDao.getPdfByIdSync(id) ?: return@withContext false
        val currentFile = File(item.filePath)
        if (!currentFile.exists()) return@withContext false

        val tempUnlockedFile = File(context.cacheDir, "unlocked_${System.currentTimeMillis()}.pdf")
        val success = PdfEngine.unlockPdf(currentFile, tempUnlockedFile, password)
        if (success && tempUnlockedFile.exists()) {
            currentFile.delete()
            tempUnlockedFile.renameTo(currentFile)
            pdfDao.updateEncryptionStatus(id, false)
            return@withContext true
        }
        false
    }

    /**
     * Share PDF file via system Intent
     */
    fun sharePdf(file: File) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Nakon-PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Open PDF file with external system viewer
     */
    fun openExternal(file: File) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(viewIntent, "Open PDF with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    /**
     * Format file size helper
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    /**
     * Format timestamp helper
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
