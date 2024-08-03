package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var tvInvoiceNumber: TextView
    private lateinit var tvCustomer: TextView
    private lateinit var tvProduct: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDate: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnEdit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        displayData()
        setupListeners()
    }

    private fun initViews() {
        tvInvoiceNumber = findViewById(R.id.tvInvoiceNumber)
        tvCustomer = findViewById(R.id.tvCustomer)
        tvProduct = findViewById(R.id.tvProduct)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvPrice = findViewById(R.id.tvPrice)
        tvDate = findViewById(R.id.tvDate)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnEdit = findViewById(R.id.btnEdit)
    }

    private fun displayData() {
        tvInvoiceNumber.text = "Invoice Number: ${intent.getStringExtra("invoiceNumber")}"
        tvCustomer.text = "Customer: ${intent.getStringExtra("customer")}"
        tvProduct.text = "Product: ${intent.getStringExtra("product")}"
        tvQuantity.text = "Quantity: ${intent.getStringExtra("quantity")}"
        tvPrice.text = "Price: ${intent.getStringExtra("price")}"
        tvDate.text = "Date: ${intent.getStringExtra("date")}"
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            sendToOdoo()
        }

        btnEdit.setOnClickListener {
            finish()
        }
    }

    private fun sendToOdoo() {
        // TODO: Implement actual Odoo API call
        val invoiceNumber = intent.getStringExtra("invoiceNumber")
        val intent = Intent(this, SuccessActivity::class.java).apply {
            putExtra("invoiceNumber", invoiceNumber)
        }
        startActivity(intent)
        finishAffinity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}