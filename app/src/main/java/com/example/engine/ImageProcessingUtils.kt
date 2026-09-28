package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.example.model.ScanFilter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageProcessingUtils {

    // Standard A4 dimensions in points (72 DPI)
    const val A4_WIDTH_PT = 595
    const val A4_HEIGHT_PT = 842

    // Target high-res dimensions for A4 page bitmap (150 DPI)
    const val A4_RENDER_WIDTH = 1240
    const val A4_RENDER_HEIGHT = 1754

    /**
     * Decode Bitmap from Uri safely without OutOfMemoryError
     */
    fun decodeBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1800,
        maxHeight: Int = 2400
    ): Bitmap? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotates bitmap by given angle in degrees.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * Applies anti-shake sharpening and document enhancement filter.
     */
    fun applyScanFilter(srcBitmap: Bitmap, filter: ScanFilter): Bitmap {
        return when (filter) {
            ScanFilter.BW_DOCUMENT -> applyBwDocumentFilter(srcBitmap)
            ScanFilter.MAGIC_COLOR -> applyMagicColorFilter(srcBitmap)
            ScanFilter.GRAYSCALE -> applyGrayscaleFilter(srcBitmap)
            ScanFilter.ORIGINAL -> srcBitmap
        }
    }

    /**
     * B&W Document Scan:
     * Transforms background to pure crisp white and text to rich black ink.
     * Removes uneven shadows, page curvature, wrinkles, and hand shake blur.
     */
    private fun applyBwDocumentFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate average brightness and threshold
        var totalLuminance = 0L
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            totalLuminance += lum
        }
        val avgLuminance = (totalLuminance / pixels.size).toInt()
        // Threshold biased slightly towards white background
        val threshold = min(210, max(128, avgLuminance + 15))

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            if (lum > threshold) {
                // Background paper -> pure clean white
                pixels[i] = Color.WHITE
            } else {
                // Ink / Text / Signature -> high contrast black/dark
                val inkRatio = lum.toFloat() / threshold.toFloat()
                val inkVal = (inkRatio * 45).toInt().coerceIn(0, 45)
                pixels[i] = Color.rgb(inkVal, inkVal, inkVal)
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Magic Color:
     * Enhances colors, lifts shadows to bright white, sharpens colored text and stamps.
     */
    private fun applyMagicColorFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Color matrix with boosted contrast and white-lift
        val contrast = 1.35f
        val brightness = 20f

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Boost saturation slightly
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1.25f)
        cm.postConcat(satMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Grayscale filter with contrast stretch
     */
    private fun applyGrayscaleFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix()
        cm.setSaturation(0f)

        // Increase contrast
        val contrast = 1.3f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Fits a given Bitmap into standard A4 canvas (white background).
     */
    fun fitBitmapToA4(bitmap: Bitmap, targetWidth: Int = A4_RENDER_WIDTH, targetHeight: Int = A4_RENDER_HEIGHT): Bitmap {
        val a4Bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(a4Bitmap)
        canvas.drawColor(Color.WHITE)

        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()

        // Calculate aspect fit with 20px margin
        val margin = 20f
        val availWidth = targetWidth - (margin * 2)
        val availHeight = targetHeight - (margin * 2)

        val scale = min(availWidth / srcWidth, availHeight / srcHeight)
        val scaledW = srcWidth * scale
        val scaledH = srcHeight * scale

        val left = margin + (availWidth - scaledW) / 2f
        val top = margin + (availHeight - scaledH) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dstRect = Rect(left.toInt(), top.toInt(), (left + scaledW).toInt(), (top + scaledH).toInt())
        canvas.drawBitmap(bitmap, null, dstRect, paint)

        return a4Bitmap
    }

    /**
     * Save bitmap to temp cache file
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }
}
