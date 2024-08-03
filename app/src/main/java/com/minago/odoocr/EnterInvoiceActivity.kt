package com.minago.odoocr

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class EnterInvoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_invoice)

        findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
            // We'll implement this later
        }

        findViewById<Button>(R.id.btnChooseFromGallery).setOnClickListener {
            // We'll implement this later
        }

        findViewById<Button>(R.id.btnEnterDataManually).setOnClickListener {
            // We'll implement this later
        }
    }
}