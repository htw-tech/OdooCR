package com.minago.odoocr

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.text.SimpleDateFormat
import java.util.*

object InvoiceProcessor {
    private const val TAG = "InvoiceProcessor"

    suspend fun processInvoice(context: Context, fileName: String): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            val bitmap = loadImageFromAssets(context, fileName)
            if (bitmap != null) {
                val preprocessedBitmap = preprocessImage(bitmap)
                val extractedText = performOCR(preprocessedBitmap)
                Log.d(TAG, "Full OCR result: \n$extractedText")
                if (extractedText.isNotEmpty()) {
                    val cleanedText = cleanUpText(extractedText)
                    Log.d(TAG, "Cleaned text: \n$cleanedText")
                    val invoiceData = extractInvoiceData(cleanedText)
                    Log.d(TAG, "Extracted invoice data: $invoiceData")
                    invoiceData
                } else {
                    Log.e(TAG, "No text extracted from OCR")
                    mapOf("error" to "No text extracted")
                }
            } else {
                Log.e(TAG, "Failed to load image")
                mapOf("error" to "Failed to load image")
            }
        }
    }

    private fun loadImageFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}")
            null
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val paint = Paint()
        val rect = Rect(0, 0, width, height)

        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        canvas.drawBitmap(bitmap, null, rect, paint)

        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            set(floatArrayOf(
                1.5f, 0f, 0f, 0f, -75f,
                0f, 1.5f, 0f, 0f, -75f,
                0f, 0f, 1.5f, 0f, -75f,
                0f, 0f, 0f, 1f, 0f
            ))
        })
        canvas.drawBitmap(newBitmap, null, rect, paint)

        return newBitmap
    }

    private suspend fun performOCR(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return suspendCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText -> continuation.resume(visionText.text) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                    continuation.resume("")
                }
        }
    }

    private fun cleanUpText(text: String): String {
        val corrections = mapOf(
            "Refi?rence" to "Référence",
            "Vante" to "Vente",
            "1Oml" to "10ml",
            "10/4?0?1?23" to "10/10/23",
            "N'?Bon" to "N°Bon",
            "DétermineA(b(b|ß|ß|b)o?t)?" to "Détermine Abbot",
            "BIOLINEH?CV" to "BIOLINE HCV",
            "Sigtagy" to "Sigraay",
            "infoOgmedis.com" to "info@gmedis.com",
            "10[.,]000[Uu]nités" to "10 000 Unités",
            "Fla?ç?ons" to "Flacons"
        )

        var cleanedText = text
        for ((pattern, replacement) in corrections) {
            cleanedText = cleanedText.replace(pattern.toRegex(RegexOption.IGNORE_CASE), replacement)
        }
        return cleanedText
    }

    private fun extractInvoiceData(text: String): Map<String, Any> {
        val lines = text.lines()
        val invoiceLines = mutableListOf<Map<String, Any>>()
        var currentProduct = mutableMapOf<String, Any>()
        var isProductSection = false

        val partnerInfo = extractPartnerInfo(text)
        val invoiceDate = extractInvoiceDate(text)

        lines.forEach { line ->
            when {
                line.contains("Désignation", ignoreCase = true) -> isProductSection = true
                isProductSection && line.contains("Arretée la présente facture", ignoreCase = true) -> isProductSection = false
                isProductSection -> {
                    if (line.contains("Référence", ignoreCase = true) && currentProduct.isNotEmpty()) {
                        invoiceLines.add(currentProduct)
                        currentProduct = mutableMapOf()
                    }
                    when {
                        line.startsWith("IDAO", ignoreCase = true) || line.startsWith("HENZO", ignoreCase = true) -> {
                            currentProduct["default_code"] = line.split(" ").first()
                            currentProduct["name"] = line.substringAfter(" ")
                        }
                        line.contains("Flacons", ignoreCase = true) || line.contains("Unités", ignoreCase = true) -> {
                            val parts = line.split(" ")
                            currentProduct["product_uom_qty"] = parts.first().toFloatOrNull() ?: 0f
                            currentProduct["uom"] = parts.last()
                        }
                        line.contains(",") -> {
                            val parts = line.split(" ")
                            if (currentProduct["price_unit"] == null) {
                                currentProduct["price_unit"] = parts.first().replace(",", ".").toFloatOrNull() ?: 0f
                            } else {
                                currentProduct["price_subtotal"] = parts.first().replace(" ", "").toFloatOrNull() ?: 0f
                            }
                        }
                    }
                }
            }
        }
        if (currentProduct.isNotEmpty()) {
            invoiceLines.add(currentProduct)
        }

        return mapOf(
            "invoice_line_ids" to invoiceLines,
            "partner_id" to partnerInfo,
            "invoice_date" to invoiceDate
        )
    }

    private fun extractPartnerInfo(text: String): String {
        val partnerRegex = "CNTS\\s+N'DJAMENA".toRegex(RegexOption.IGNORE_CASE)
        return partnerRegex.find(text)?.value ?: "Unknown Partner"
    }

    private fun extractInvoiceDate(text: String): String {
        val dateRegex = "\\b\\d{2}/\\d{2}/\\d{2}\\b".toRegex()
        val match = dateRegex.find(text)
        return if (match != null) {
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val date = dateFormat.parse(match.value)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date ?: Date())
        } else {
            ""
        }
    }
}