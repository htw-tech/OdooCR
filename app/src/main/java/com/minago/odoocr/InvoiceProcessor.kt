package com.minago.odoocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object InvoiceProcessor {
    private const val TAG = "InvoiceProcessor"

    suspend fun processInvoice(context: Context, fileName: String): Map<String, String> {
        return withContext(Dispatchers.Default) {
            val bitmap = loadImageFromAssets(context, fileName)
            if (bitmap != null) {
                val extractedText = performOCR(bitmap)
                extractFields(extractedText)
            } else {
                mapOf("error" to "Failed to load image")
            }
        }
    }

    private fun loadImageFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use {
                BitmapFactory.decodeStream(it)
            }
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
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                    continuation.resume("")
                }
        }
    }

    private fun extractFields(text: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        var designationFound = false
        var designationCount = 0

        text.lines().forEach { line ->
            when {
                line.startsWith("Désignation", ignoreCase = true) -> {
                    designationFound = true
                }
                designationFound && line.isNotBlank() && !line.endsWith(":") -> {
                    designationCount++
                    fields["Désignation$designationCount"] = line.trim()
                }
                line.startsWith("Qté", ignoreCase = true) ||
                        line.startsWith("P.U", ignoreCase = true) ||
                        line.startsWith("Montant HT", ignoreCase = true) -> {
                    designationFound = false
                }
            }
        }

        return fields
    }
}