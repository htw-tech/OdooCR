package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/*! \class MainActivity
    \brief Main activity of the application which serves as the entry point.
 */
class MainActivity : AppCompatActivity() {

    /*! \brief Called when the activity is starting.
        \param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnEnterInvoice).setOnClickListener {
            startActivity(Intent(this, EnterInvoiceActivity::class.java))
        }
    }
}