package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnEnterInvoice).setOnClickListener {
            startActivity(Intent(this, EnterInvoiceActivity::class.java))
        }

        findViewById<Button>(R.id.btnScannedInvoices).setOnClickListener {
            // We'll implement this later
        }
    }
}