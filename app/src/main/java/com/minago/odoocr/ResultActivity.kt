package com.minago.odoocr

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Button

/*! \class ResultActivity
    \brief Activity to display the result of the invoice processing.
 */
class ResultActivity : AppCompatActivity() {

    /*! \brief Called when the activity is starting.
        \param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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

    /*! \brief Handle options item selected.
        \param item The selected menu item.
        \return true if the item selection was handled, false otherwise.
     */
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