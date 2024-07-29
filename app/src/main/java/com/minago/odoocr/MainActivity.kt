package com.minago.odoocr

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var invoiceProcessor: InvoiceProcessor
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        invoiceProcessor = InvoiceProcessor(this)

        lifecycleScope.launch {
            processInvoiceTemplate()
        }
    }

    private suspend fun processInvoiceTemplate() {
        try {
            val inputStream = assets.open("invoice_template.jpg")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val result = withContext(Dispatchers.Default) {
                invoiceProcessor.processInvoice(bitmap)
            }

            resultTextView.text = getString(R.string.result_text, result)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error processing invoice", e)
            resultTextView.text = getString(R.string.error_text, e.message)
        }
    }
}
