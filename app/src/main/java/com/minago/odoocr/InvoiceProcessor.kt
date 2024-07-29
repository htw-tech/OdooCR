package com.minago.odoocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
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

        for (line in lines) {
            when {
                line.contains("CNTS") -> partnerId = line.trim()
                line.contains("Date") -> invoiceDate = extractDate(line)
                line.matches(Regex("^[A-Z]+\\d+.*")) -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 6) {
                        invoiceLines.add(mapOf(
                            "default_code" to parts[0],
                            "name" to parts.subList(1, parts.size - 4).joinToString(" "),
                            "product_uom_qty" to (parts[parts.size - 4].replace(",", ".").toDoubleOrNull() ?: 0.0),
                            "uom" to parts[parts.size - 3],
                            "price_unit" to (parts[parts.size - 2].replace(",", ".").toDoubleOrNull() ?: 0.0),
                            "price_subtotal" to (parts.last().replace(",", ".").toDoubleOrNull() ?: 0.0)
                        ))
                    }
                }
            }
        }

        val jsonObject = JSONObject()
        jsonObject.put("invoice_line_ids", JSONArray(invoiceLines))
        jsonObject.put("partner_id", partnerId)
        jsonObject.put("invoice_date", invoiceDate)

        return jsonObject.toString(4)  // Use 4 spaces for indentation in JSON output
    }

    private fun extractDate(line: String): String {
        val datePart = line.split(":").lastOrNull()?.trim() ?: return ""
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val parsedDate = dateFormat.parse(datePart)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
        } catch (e: ParseException) {
            Log.e("InvoiceProcessor", "Error parsing date: $datePart", e)
            ""
        }
    }
}