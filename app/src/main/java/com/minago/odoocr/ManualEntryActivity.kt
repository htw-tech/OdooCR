package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ManualEntryActivity : AppCompatActivity() {

    private lateinit var etInvoiceNumber: EditText
    private lateinit var etCustomer: EditText
    private lateinit var etProduct: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDate: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button

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
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnSubmit.setOnClickListener {
            submitData()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun submitData() {
        if (validateInputs()) {
            val intent = Intent(this, ConfirmationActivity::class.java).apply {
                putExtra("invoiceNumber", etInvoiceNumber.text.toString())
                putExtra("customer", etCustomer.text.toString())
                putExtra("product", etProduct.text.toString())
                putExtra("quantity", etQuantity.text.toString())
                putExtra("price", etPrice.text.toString())
                putExtra("date", etDate.text.toString())
            }
            startActivity(intent)
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (etInvoiceNumber.text.toString().isEmpty()) {
            etInvoiceNumber.error = "Invoice number is required"
            isValid = false
        }
        if (etCustomer.text.toString().isEmpty()) {
            etCustomer.error = "Customer is required"
            isValid = false
        }
        if (etProduct.text.toString().isEmpty()) {
            etProduct.error = "Product is required"
            isValid = false
        }
        if (etQuantity.text.toString().isEmpty()) {
            etQuantity.error = "Quantity is required"
            isValid = false
        }
        if (etPrice.text.toString().isEmpty()) {
            etPrice.error = "Price is required"
            isValid = false
        }
        if (etDate.text.toString().isEmpty()) {
            etDate.error = "Date is required"
            isValid = false
        }

        if (!isValid) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }

        return isValid
    }
}