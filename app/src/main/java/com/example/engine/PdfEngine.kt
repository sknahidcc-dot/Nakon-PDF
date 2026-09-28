package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.model.CapturedPage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfEngine {

    const val A4_PAGE_WIDTH_PT = 595
    const val A4_PAGE_HEIGHT_PT = 842

    data class GenerationResult(
        val success: Boolean,
        val outputFile: File?,
        val pageCount: Int,
        val fileSizeBytes: Long,
        val thumbnailFile: File?,
        val errorMessage: String? = null
    )

    /**
     * Create an A4 PDF from a list of captured pages with filters applied.
     */
    suspend fun createPdf(
        context: Context,
        pages: List<CapturedPage>,
        outputFile: File,
        password: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): GenerationResult = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            return@withContext GenerationResult(false, null, 0, 0L, null, "No pages to convert")
        }

        val tempUnprotectedFile = if (!password.isNullOrBlank()) {
            File(context.cacheDir, "temp_build_${System.currentTimeMillis()}.pdf")
        } else {
            outputFile
        }

        val pdfDocument = PdfDocument()
        var thumbnailBitmap: Bitmap? = null

        try {
            for (index in pages.indices) {
                val page = pages[index]
                onProgress(index + 1, pages.size)

                // Decode raw image
                val rawBitmap = ImageProcessingUtils.decodeBitmapFromUri(context, page.imageUri)
                    ?: continue

                // Rotate if required
                val rotatedBitmap = if (page.rotationDegrees != 0) {
                    ImageProcessingUtils.rotateBitmap(rawBitmap, page.rotationDegrees.toFloat())
                } else {
                    rawBitmap
                }

                // Apply scan filter (e.g. B&W Document Scan, Magic Color, Grayscale)
                val processedBitmap = ImageProcessingUtils.applyScanFilter(rotatedBitmap, page.filter)

                // Create A4 PDF Page
                val pageInfo = PdfDocument.PageInfo.Builder(A4_PAGE_WIDTH_PT, A4_PAGE_HEIGHT_PT, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = pdfPage.canvas

                // Fill A4 background white
                canvas.drawColor(Color.WHITE)

                // Calculate aspect fit to A4 page dimensions
                val margin = 16f
                val printableWidth = A4_PAGE_WIDTH_PT - (margin * 2)
                val printableHeight = A4_PAGE_HEIGHT_PT - (margin * 2)

                val scale = min(printableWidth / processedBitmap.width.toFloat(), printableHeight / processedBitmap.height.toFloat())
                val drawW = processedBitmap.width * scale
                val drawH = processedBitmap.height * scale

                val left = margin + (printableWidth - drawW) / 2f
                val top = margin + (printableHeight - drawH) / 2f

                val destRect = Rect(left.toInt(), top.toInt(), (left + drawW).toInt(), (top + drawH).toInt())
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(processedBitmap, null, destRect, paint)

                pdfDocument.finishPage(pdfPage)

                // Capture first page thumbnail for quick UI list loading
                if (index == 0) {
                    val thumbW = 200
                    val thumbH = (200 * (A4_PAGE_HEIGHT_PT.toFloat() / A4_PAGE_WIDTH_PT.toFloat())).toInt()
                    thumbnailBitmap = Bitmap.createScaledBitmap(processedBitmap, thumbW, thumbH, true)
                }

                // Clean up bitmaps if recycled
                if (processedBitmap != rotatedBitmap && !processedBitmap.isRecycled) {
                    processedBitmap.recycle()
                }
                if (rotatedBitmap != rawBitmap && !rotatedBitmap.isRecycled) {
                    rotatedBitmap.recycle()
                }
                if (!rawBitmap.isRecycled) {
                    rawBitmap.recycle()
                }
            }

            // Write PDF Document
            FileOutputStream(tempUnprotectedFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }
            pdfDocument.close()

            // Save thumbnail image
            val thumbnailFile = File(context.filesDir, "thumb_${outputFile.nameWithoutExtension}.jpg")
            thumbnailBitmap?.let { tb ->
                FileOutputStream(thumbnailFile).use { out ->
                    tb.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                tb.recycle()
            }

            // If user requested password protection (PDF Lock)
            if (!password.isNullOrBlank()) {
                val lockSuccess = lockPdf(tempUnprotectedFile, outputFile, password)
                tempUnprotectedFile.delete()
                if (!lockSuccess) {
                    return@withContext GenerationResult(false, null, 0, 0L, null, "Failed to encrypt PDF")
                }
            }

            return@withContext GenerationResult(
                success = true,
                outputFile = outputFile,
                pageCount = pages.size,
                fileSizeBytes = outputFile.length(),
                thumbnailFile = if (thumbnailFile.exists()) thumbnailFile else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return@withContext GenerationResult(false, null, 0, 0L, null, e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Locks / Encrypts a PDF with standard password protection using Apache PDFBox
     */
    suspend fun lockPdf(sourceFile: File, outputFile: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = PDDocument.load(sourceFile)
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = 128
                permissions = ap
            }
            doc.protect(spp)
            doc.save(outputFile)
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Unlocks / Decrypts a password protected PDF
     */
    suspend fun unlockPdf(sourceFile: File, outputFile: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = PDDocument.load(sourceFile, password)
            if (doc.isEncrypted) {
                doc.isAllSecurityToBeRemoved = true
            }
            doc.save(outputFile)
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if a PDF file is encrypted
     */
    suspend fun isPdfEncrypted(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        try {
            val doc = PDDocument.load(file)
            val isEnc = doc.isEncrypted
            doc.close()
            isEnc
        } catch (e: InvalidPasswordException) {
            true
        } catch (e: Exception) {
            // Also check with PdfRenderer to see if security exception is thrown
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd).close()
                pfd.close()
                false
            } catch (se: SecurityException) {
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Get total page count of a PDF file
     */
    suspend fun getPageCount(file: File, password: String? = null): Int = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0
        try {
            if (!password.isNullOrBlank()) {
                val doc = PDDocument.load(file, password)
                val count = doc.numberOfPages
                doc.close()
                return@withContext count
            } else {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                pfd.close()
                return@withContext count
            }
        } catch (e: InvalidPasswordException) {
            return@withContext -1 // Indicates password required
        } catch (e: Exception) {
            // Fallback to PDFBox
            try {
                val doc = if (password != null) PDDocument.load(file, password) else PDDocument.load(file)
                val count = doc.numberOfPages
                doc.close()
                return@withContext count
            } catch (_: Exception) {
                return@withContext 0
            }
        }
    }

    /**
     * Render a specific page of a PDF file to a Bitmap for high performance viewer
     */
    suspend fun renderPageToBitmap(
        context: Context,
        file: File,
        pageIndex: Int,
        password: String? = null,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        var tempDecryptedFile: File? = null
        try {
            val actualFileToRender: File
            if (!password.isNullOrBlank()) {
                // Decrypt temporarily into cache for hardware accelerated PdfRenderer
                tempDecryptedFile = File(context.cacheDir, "view_temp_${System.currentTimeMillis()}.pdf")
                val doc = PDDocument.load(file, password)
                doc.isAllSecurityToBeRemoved = true
                doc.save(tempDecryptedFile)
                doc.close()
                actualFileToRender = tempDecryptedFile
            } else {
                actualFileToRender = file
            }

            val pfd = ParcelFileDescriptor.open(actualFileToRender, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val renderHeight = ((targetWidth.toFloat() / page.width.toFloat()) * page.height).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, renderHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            tempDecryptedFile?.delete()
        }
    }
}
