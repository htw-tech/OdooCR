package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class ManualEntryActivity : AppCompatActivity() {

    private lateinit var etInvoiceNumber: EditText
    private lateinit var etCustomer: EditText
    private lateinit var etProduct: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDate: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etInvoiceNumber = findViewById(R.id.etInvoiceNumber)
        etCustomer = findViewById(R.id.etCustomer)
        etProduct = findViewById(R.id.etProduct)
        etQuantity = findViewById(R.id.etQuantity)
        etPrice = findViewById(R.id.etPrice)
        etDate = findViewById(R.id.etDate)
        btnSubmit = findViewById(R.id.btnSubmit)
    }
    private fun setupListeners() {
        btnSubmit.setOnClickListener {
            submitData()
        }
    }

    private fun submitData() {
        val invoiceNumber = etInvoiceNumber.text.toString()
        val customer = etCustomer.text.toString()
        val product = etProduct.text.toString()
        val quantity = etQuantity.text.toString()
        val price = etPrice.text.toString()
        val date = etDate.text.toString()

        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra("invoiceNumber", invoiceNumber)
            putExtra("customer", customer)
            putExtra("product", product)
            putExtra("quantity", quantity)
            putExtra("price", price)
            putExtra("date", date)
        }
        startActivity(intent)
    }
}