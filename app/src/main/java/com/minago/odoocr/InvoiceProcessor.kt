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
                val extractedText = performOCR(bitmap)
                Log.d(TAG, "Full OCR result: \n$extractedText")
                if (extractedText.isNotEmpty()) {
                    val cleanedText = cleanUpText(extractedText)
                    Log.d(TAG, "Cleaned text: \n$cleanedText")
                    extractInvoiceData(cleanedText)
                } else {
                    mapOf("error" to "No text extracted")
                }
            } else {
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

        lines.forEach { line ->
            when {
                line.contains("Désignation", ignoreCase = true) -> isProductSection = true
                line.contains("Arretée la présente facture", ignoreCase = true) -> isProductSection = false
                isProductSection -> {
                    when {
                        line.startsWith("IDAO", ignoreCase = true) || line.startsWith("HENZO", ignoreCase = true) -> {
                            if (currentProduct.isNotEmpty()) {
                                invoiceLines.add(currentProduct)
                            }
                            currentProduct = mutableMapOf()
                            val parts = line.split("""\s+""".toRegex(), 2)
                            currentProduct["default_code"] = parts[0]
                            currentProduct["name"] = parts.getOrNull(1) ?: ""
                        }
                        line.matches("""^\d+\s+(Flacons|Unités)""".toRegex(RegexOption.IGNORE_CASE)) -> {
                            val parts = line.split("""\s+""".toRegex())
                            currentProduct["product_uom_qty"] = parts[0].toFloatOrNull() ?: 0f
                            currentProduct["uom"] = parts[1]
                        }
                        line.matches("""^\d+([.,]\d+)?\s+\d+([.,]\d+)?$""".toRegex()) -> {
                            val parts = line.split("""\s+""".toRegex())
                            currentProduct["price_unit"] = parts[0].replace(",", ".").toFloatOrNull() ?: 0f
                            currentProduct["price_subtotal"] = parts[1].replace(",", ".").toFloatOrNull() ?: 0f
                        }
                    }
                }
            }
        }
        if (currentProduct.isNotEmpty()) {
            invoiceLines.add(currentProduct)
        }

        Log.d(TAG, "Extracted invoice lines: $invoiceLines")

        return mapOf(
            "invoice_line_ids" to invoiceLines,
            "partner_id" to extractPartnerInfo(text),
            "invoice_date" to extractInvoiceDate(text)
        )
    }

    private fun extractPartnerInfo(text: String): String {
        val partnerRegex = "CNTS\\s+N'DJAMENA".toRegex(RegexOption.IGNORE_CASE)
        return partnerRegex.find(text)?.value ?: "Unknown Partner"
    }

    private fun extractInvoiceDate(text: String): String {
        val dateRegex = "Date\\s*:\\s*(\\d{2}/\\d{2}/\\d{2})".toRegex(RegexOption.IGNORE_CASE)
        val match = dateRegex.find(text)
        return if (match != null) {
            val dateStr = match.groupValues[1]
            val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } else {
            ""
        }
    }
}