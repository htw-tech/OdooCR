package com.minago.odoocr

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Button

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val resultTextView: TextView = findViewById(R.id.resultTextView)
        val result = intent.getStringExtra("INVOICE_RESULT")
        resultTextView.text = result

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}