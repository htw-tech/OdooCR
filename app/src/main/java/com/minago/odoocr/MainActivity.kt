package com.minago.odoocr

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var invoiceProcessor: InvoiceProcessor
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        invoiceProcessor = InvoiceProcessor(this)
        resultTextView = findViewById(R.id.tvResult)

        findViewById<Button>(R.id.btnEnterInvoice).setOnClickListener {
            startActivity(Intent(this, EnterInvoiceActivity::class.java))
        }

        findViewById<Button>(R.id.btnScannedInvoices).setOnClickListener {
            // Existing functionality for scanned invoices
        }

        findViewById<Button>(R.id.btnProcessTemplate).setOnClickListener {
            lifecycleScope.launch {
                processInvoiceTemplate()
            }
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

            resultTextView.text = "Template processing result: $result"
        } catch (e: Exception) {
            Log.e("MainActivity", "Error processing invoice template", e)
            resultTextView.text = "Error processing template: ${e.message}"
        }
    }
}