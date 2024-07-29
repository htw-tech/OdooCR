package com.minago.odoocr

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class InvoiceProcessor(context: Context) {
    private val tesseractOCR = TesseractOCR(context)

    fun processInvoice(bitmap: Bitmap): String {
        val ocrResult = tesseractOCR.getOCRResult(bitmap)
        return if (ocrResult.isNotBlank()) {
            parseInvoiceData(ocrResult)
        } else {
            "{\"error\": \"No text detected in image\"}"
        }
    }

    private fun parseInvoiceData(ocrText: String): String {
        val lines = ocrText.split("\n")
        val invoiceLines = mutableListOf<Map<String, Any>>()
        var partnerId = ""
        var invoiceDate = ""
        var invoiceNumber = ""

        for (line in lines) {
            when {
                line.contains("Date") -> invoiceDate = extractDate(line)
                line.contains("Numéro") -> invoiceNumber = extractInvoiceNumber(line)
                line.matches(Regex("^[A-Z]+\\d+.*")) -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 6) {
                        invoiceLines.add(
                            mapOf(
                                "name" to parts.subList(1, parts.size - 4).joinToString(" "),
                                "product_uom_qty" to (parts[parts.size - 4].replace(",", ".")
                                    .toDoubleOrNull() ?: 0.0),
                                "price_unit" to (parts[parts.size - 2].replace(",", ".")
                                    .toDoubleOrNull() ?: 0.0),
                                "price_subtotal" to (parts.last().replace(",", ".").toDoubleOrNull()
                                    ?: 0.0)
                            )
                        )
                    }
                }
            }
        }

        val jsonObject = JSONObject()
        jsonObject.put("invoice_line_ids", JSONArray(invoiceLines))
        jsonObject.put("invoice_date", invoiceDate)
        jsonObject.put("invoice_number", invoiceNumber)

        return jsonObject.toString(4)
    }

    private fun extractDate(line: String): String {
        val datePattern = "\\b\\d{2}/\\d{2}/\\d{2}\\b".toRegex()
        val match = datePattern.find(line)
        return if (match != null) {
            val datePart = match.value
            try {
                val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(datePart)
                outputFormat.format(date)
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    private fun extractInvoiceNumber(line: String): String {
        val numberPattern = "\\b\\d+\\b".toRegex()
        val match = numberPattern.find(line)
        return match?.value ?: ""
    }
}