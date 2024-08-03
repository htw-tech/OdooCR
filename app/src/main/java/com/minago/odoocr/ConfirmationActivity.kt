package com.minago.odoocr

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfirmationActivity : AppCompatActivity() {

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

        initViews()
        displayData()
        setupListeners()
    }

    private fun initViews() {
        tvCustomer = findViewById(R.id.tvCustomer)
        tvProduct = findViewById(R.id.tvProduct)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvPrice = findViewById(R.id.tvPrice)
        tvDate = findViewById(R.id.tvDate)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnEdit = findViewById(R.id.btnEdit)
    }

    private fun displayData() {
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
        // TODO: Implement Odoo API call
        Toast.makeText(this, "Data sent to Odoo", Toast.LENGTH_SHORT).show()
        // Navigate back to main screen or show success message
        finish()
    }
}