package com.cashq.core.sharing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android Intent and MediaStore dispatcher for sharing and saving CashQ summaries.
 */
object ShareManager {

    /**
     * Shares Unicode/emoji formatted plain text via the Android Sharesheet.
     */
    fun shareRichText(context: Context, data: CashCountShareData) {
        val summary = RichTextShareFormatter.format(data)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, data.title)
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Cash Summary")
        context.startActivity(chooser)
    }

    /**
     * Composes an email with rich HTML formatting and a readable plain-text fallback.
     */
    fun shareEmail(context: Context, data: CashCountShareData) {
        val subject = EmailShareFormatter.formatSubject(data)
        val htmlBody = EmailShareFormatter.formatHtml(data)
        val plainFallback = RichTextShareFormatter.format(data)

        // Try ACTION_SENDTO with mailto: to target email clients directly
        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
            this.data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, plainFallback)
            putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
        }

        try {
            if (sendToIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(sendToIntent)
                return
            }
        } catch (_: Exception) {
        }

        // Fallback: ACTION_SEND with text/html or general chooser
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, plainFallback)
            putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
        }

        try {
            val chooser = Intent.createChooser(fallbackIntent, "Send Cash Summary via Email")
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "No email application found on device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the rendered summary card bitmap to the public Pictures/CashQ collection using MediaStore.
     */
    fun saveCardImage(context: Context, bitmap: Bitmap, data: CashCountShareData): Result<Uri> {
        return try {
            val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
            val dateStr = fileDateFormat.format(Date(data.timestamp))
            val baseName = "CashQ_Summary_$dateStr"
            val fileName = "$baseName.png"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CashQ")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(IllegalStateException("Failed to create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("Failed to compress bitmap to PNG")
                }
            } ?: return Result.failure(IllegalStateException("Failed to open MediaStore output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the rendered summary card bitmap via Android Sharesheet using a secure FileProvider content URI.
     */
    fun shareCardImage(context: Context, bitmap: Bitmap, data: CashCountShareData): Result<Unit> {
        return try {
            val imagesDir = File(context.cacheDir, "shared_images").apply {
                if (!exists()) mkdirs()
            }
            val cacheFile = File(imagesDir, "CashQ_Summary.png")

            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, data.title)
                putExtra(Intent.EXTRA_TEXT, "${data.title} · ${data.formattedTotalRupees} (${data.formattedTotalUnits} units)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Summary Card").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
