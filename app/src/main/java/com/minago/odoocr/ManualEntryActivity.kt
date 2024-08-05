package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/*! \class ManualEntryActivity
    \brief Activity for manually entering invoice details.
 */
class ManualEntryActivity : AppCompatActivity() {

    private lateinit var etInvoiceNumber: EditText
    private lateinit var etCustomer: EditText
    private lateinit var etProduct: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDate: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button

    /*! \brief Called when the activity is starting.
        \param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        initViews()
        setupListeners()
    }

    /*! \brief Initialize views by finding them by their ID. */
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

    /*! \brief Set up listeners for the buttons. */
    private fun setupListeners() {
        btnSubmit.setOnClickListener {
            submitData()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    /*! \brief Submits the data if inputs are valid. */
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

    /*! \brief Validates the inputs entered by the user.
        \return True if all inputs are valid, false otherwise.
     */
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
