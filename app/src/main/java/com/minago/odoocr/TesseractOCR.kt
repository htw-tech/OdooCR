package com.minago.odoocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TesseractOCR(private val context: Context) {
    private val tessBaseApi: TessBaseAPI = TessBaseAPI()

    init {
        val dataPath = File(context.filesDir, "tesseract")
        if (!dataPath.exists()) dataPath.mkdirs()
        val tessDataFolder = File(dataPath, "tessdata")
        if (!tessDataFolder.exists()) tessDataFolder.mkdirs()

        val trainedDataFile = File(tessDataFolder, "fra.traineddata")
        if (!trainedDataFile.exists()) {
            try {
                val input = context.assets.open("tessdata/fra.traineddata")
                val output = FileOutputStream(trainedDataFile)
                input.copyTo(output)
                input.close()
                output.close()
            } catch (e: IOException) {
                Log.e("TesseractOCR", "Error copying trained data", e)
            }
        }

        if (!tessBaseApi.init(dataPath.absolutePath, "fra")) {
            Log.e("TesseractOCR", "Couldn't initialize Tesseract")
        }
    }

    fun getOCRResult(bitmap: Bitmap): String {
        Log.d("TesseractOCR", "Processing image: ${bitmap.width}x${bitmap.height}")
        tessBaseApi.setImage(bitmap)
        val result = tessBaseApi.utF8Text ?: ""
        Log.d("TesseractOCR", "OCR Result: $result")
        return result
    }

    fun cleanup() {
        tessBaseApi.end()
    }
}