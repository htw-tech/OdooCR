package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import de.timroes.axmlrpc.XMLRPCClient
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/*! \class ConfirmationActivity
    \brief Activity for confirming invoice details before sending to Odoo.
 */
class ConfirmationActivity : AppCompatActivity() {

    private lateinit var tvInvoiceNumber: TextView
    private lateinit var tvCustomer: TextView
    private lateinit var tvProduct: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvBarcode: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnEdit: Button

    private val TAG = "ConfirmationActivity"
    private val ODOO_URL = "http://192.168.178.114:8016"
    private val DB = "odoo"
    private val ADMIN_UID = 2
    private val ADMIN_PASSWORD = "admin"

    /*! \brief Called when the activity is starting.
        \param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        displayData()
        setupListeners()
    }

    /*! \brief Initialize views by finding them by their ID. */
    private fun initViews() {
        tvInvoiceNumber = findViewById(R.id.tvInvoiceNumber)
        tvCustomer = findViewById(R.id.tvCustomer)
        tvProduct = findViewById(R.id.tvProduct)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvPrice = findViewById(R.id.tvPrice)
        tvDate = findViewById(R.id.tvDate)
        tvBarcode = findViewById(R.id.tvBarcode)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnEdit = findViewById(R.id.btnEdit)
    }

    /*! \brief Display data passed from the previous activity. */
    private fun displayData() {
        intent.apply {
            tvInvoiceNumber.text = "Invoice Number: ${getStringExtra("invoiceNumber")}"
            tvCustomer.text = "Customer: ${getStringExtra("customer")}"
            tvProduct.text = "Product: ${getStringExtra("product")}"
            tvQuantity.text = "Quantity: ${getStringExtra("quantity")}"
            tvPrice.text = "Price: ${getStringExtra("price")}"
            tvDate.text = "Date: ${getStringExtra("date")}"
        }

        lifecycleScope.launch {
            val productName = intent.getStringExtra("product") ?: ""
            val productBarcode = searchProductBarcode(productName)
            tvBarcode.text = if (productBarcode.isNotEmpty()) {
                "Product Barcode: $productBarcode"
            } else {
                "No barcode found for the product $productName"
            }
            tvBarcode.visibility = TextView.VISIBLE
        }
    }

    /*! \brief Set up listeners for the buttons. */
    private fun setupListeners() {
        btnConfirm.setOnClickListener { sendToOdoo() }
        btnEdit.setOnClickListener { finish() }
    }

    /*! \brief Sends the invoice data to Odoo. */
    private fun sendToOdoo() {
        lifecycleScope.launch {
            try {
                val uid = authenticateUser()
                val date = validateDate(intent.getStringExtra("date"))
                val customerName = intent.getStringExtra("customer") ?: ""
                val productName = intent.getStringExtra("product") ?: ""
                val userInvoiceNumber = intent.getStringExtra("invoiceNumber") ?: ""

                if (isInvoiceNumberExists(userInvoiceNumber)) {
                    Toast.makeText(this@ConfirmationActivity, "Error: Invoice Number already exists!", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val customerId = getOrCreateCustomer(customerName)
                val productId = getOrCreateProduct(productName)
                val productBarcode = searchProductBarcode(productName)

                Log.d(TAG, "Invoice Data - Customer: $customerId, Product: $productId, Barcode: $productBarcode, Quantity: ${intent.getStringExtra("quantity")}, Price: ${intent.getStringExtra("price")}, Date: $date, Invoice Number: $userInvoiceNumber")

                val invoiceId = createInvoice(uid, customerId, productId, productBarcode, date, userInvoiceNumber)

                Log.d(TAG, "Invoice created with ID: $invoiceId")

                navigateToSuccessActivity(userInvoiceNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invoice", e)
                Toast.makeText(this@ConfirmationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /*! \brief Check if the invoice number already exists in Odoo.
        \param invoiceNumber The invoice number to check.
        \return True if the invoice number exists, false otherwise.
     */
    private suspend fun isInvoiceNumberExists(invoiceNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ODOO_URL/xmlrpc/2/object")
            val client = XMLRPCClient(url)
            val result = client.call(
                "execute_kw", DB, ADMIN_UID, ADMIN_PASSWORD, "account.move", "search",
                listOf(listOf(listOf("name", "=", invoiceNumber))),
                mapOf("limit" to 1)
            )
            result is Array<*> && result.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking invoice number: $invoiceNumber", e)
            false
        }
    }

    /*! \brief Authenticate the user to Odoo.
        \return The user ID.
     */
    private suspend fun authenticateUser(): Int = withContext(Dispatchers.IO) {
        val url = URL("$ODOO_URL/xmlrpc/2/common")
        val client = XMLRPCClient(url)
        client.call("authenticate", DB, "mh.rouissi@gmail.com", ADMIN_PASSWORD, emptyList<Any>()) as Int
    }

    /*! \brief Validate the date format.
        \param dateString The date string to validate.
        \return The validated date string.
        \throws IllegalArgumentException If the date format is invalid.
     */
    private fun validateDate(dateString: String?): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateString?.let {
            try {
                dateFormat.parse(it)
                it
            } catch (e: Exception) {
                Log.e(TAG, "Invalid date format: $it", e)
                throw IllegalArgumentException("Invalid date format. Please use 'yyyy-MM-dd'.")
            }
        } ?: throw IllegalArgumentException("Date is required.")
    }

    /*! \brief Get or create a customer in Odoo.
        \param customerName The name of the customer.
        \return The customer ID.
     */
    private suspend fun getOrCreateCustomer(customerName: String): Int = withContext(Dispatchers.IO) {
        searchOdooRecord("res.partner", customerName) ?: createOdooRecord("res.partner", mapOf("name" to customerName))
        ?: throw Exception("Failed to create customer: $customerName")
    }

    /*! \brief Get or create a product in Odoo.
        \param productName The name of the product.
        \return The product ID.
     */
    private suspend fun getOrCreateProduct(productName: String): Int = withContext(Dispatchers.IO) {
        searchOdooRecord("product.product", productName) ?: createOdooRecord("product.product", mapOf("name" to productName))
        ?: throw Exception("Failed to create product: $productName")
    }

    /*! \brief Search for a record in Odoo.
        \param model The model to search in.
        \param name The name of the record.
        \return The record ID if found, null otherwise.
     */
    private suspend fun searchOdooRecord(model: String, name: String): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ODOO_URL/xmlrpc/2/object")
            val client = XMLRPCClient(url)
            val result = client.call(
                "execute_kw", DB, ADMIN_UID, ADMIN_PASSWORD, model, "name_search",
                listOf(name), mapOf("limit" to 1)
            )
            if (result is Array<*>) {
                val record = result.firstOrNull() as? Array<*>
                record?.get(0) as? Int
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching $model: $name", e)
            null
        }
    }

    /*! \brief Create a record in Odoo.
        \param model The model to create in.
        \param values The values to set in the new record.
        \return The new record ID if created, null otherwise.
     */
    private suspend fun createOdooRecord(model: String, values: Map<String, Any?>): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ODOO_URL/xmlrpc/2/object")
            val client = XMLRPCClient(url)
            client.call("execute_kw", DB, ADMIN_UID, ADMIN_PASSWORD, model, "create", listOf(values)) as? Int
        } catch (e: Exception) {
            Log.e(TAG, "Error creating $model: $values", e)
            null
        }
    }

    /*! \brief Search for the barcode of a product.
        \param productName The name of the product.
        \return The barcode if found, an empty string otherwise.
     */
    private suspend fun searchProductBarcode(productName: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ODOO_URL/xmlrpc/2/object")
            val client = XMLRPCClient(url)
            val result = client.call(
                "execute_kw", DB, ADMIN_UID, ADMIN_PASSWORD, "product.product", "search_read",
                listOf(listOf(listOf("name", "=", productName))),
                mapOf("fields" to listOf("barcode"), "limit" to 1)
            )
            if (result is Array<*>) {
                val product = result.firstOrNull() as? Map<*, *>
                product?.get("barcode") as? String ?: ""
            } else ""
        } catch (e: Exception) {
            Log.e(TAG, "Error searching product barcode: $productName", e)
            ""
        }
    }

    /*! \brief Create an invoice in Odoo.
        \param uid The user ID.
        \param customerId The customer ID.
        \param productId The product ID.
        \param productBarcode The product barcode.
        \param date The invoice date.
        \param invoiceNumber The invoice number.
        \return The invoice ID if created.
        \throws Exception If the invoice creation fails.
     */
    private suspend fun createInvoice(uid: Int, customerId: Int, productId: Int, productBarcode: String, date: String, invoiceNumber: String): Int = withContext(Dispatchers.IO) {
        val url = URL("$ODOO_URL/xmlrpc/2/object")
        val client = XMLRPCClient(url)

        val invoiceVals = mapOf(
            "move_type" to "out_invoice",
            "partner_id" to customerId,
            "invoice_date" to date,
            "name" to invoiceNumber,  // This sets the invoice number
            "invoice_line_ids" to listOf(listOf(
                0, 0, mapOf(
                    "product_id" to productId,
                    "quantity" to intent.getStringExtra("quantity")?.toDoubleOrNull(),
                    "price_unit" to intent.getStringExtra("price")?.toDoubleOrNull(),
                    "barcode_scan" to productBarcode
                )
            ))
        )

        Log.d(TAG, "Invoice Vals: $invoiceVals")

        val result = client.call("execute_kw", DB, uid, ADMIN_PASSWORD, "account.move", "create", listOf(invoiceVals))
        result as? Int ?: throw Exception("Invoice creation failed: Unexpected response")
    }

    /*! \brief Navigate to the success activity.
        \param invoiceNumber The invoice number.
     */
    private fun navigateToSuccessActivity(invoiceNumber: String) {
        val intent = Intent(this@ConfirmationActivity, SuccessActivity::class.java).apply {
            putExtra("invoiceNumber", invoiceNumber)
        }
        startActivity(intent)
        finishAffinity()
    }

    /*! \brief Handle options item selected.
        \param item The selected menu item.
        \return true if the item selection was handled, false otherwise.
     */
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
