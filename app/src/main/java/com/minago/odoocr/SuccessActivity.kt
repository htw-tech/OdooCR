package com.minago.odoocr

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class SuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        val tvSuccessMessage = findViewById<TextView>(R.id.tvSuccessMessage)
        val invoiceNumber = intent.getStringExtra("invoiceNumber")
        tvSuccessMessage.text = "Invoice $invoiceNumber has been transmitted successfully to Odoo."

        findViewById<Button>(R.id.btnNewEntry).setOnClickListener {
            val intent = Intent(this, ManualEntryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            finishAffinity()
        }
    }
}