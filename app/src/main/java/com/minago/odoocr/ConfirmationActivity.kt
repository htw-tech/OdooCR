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
        tvBarcode = findViewById(R.id.tvBarcode)
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

        lifecycleScope.launch {
            val productName = intent.getStringExtra("product") ?: ""
            val productBarcode = searchProductBarcode(productName)
            if (productBarcode.isNotEmpty()) {
                tvBarcode.text = "Product Barcode: $productBarcode"
                tvBarcode.visibility = TextView.VISIBLE
            } else {
                tvBarcode.text = "No barcode found for the product $productName"
                tvBarcode.visibility = TextView.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            sendToOdoo()
        }

        btnEdit.setOnClickListener {
            finish()
        }
    }

    private suspend fun searchOdooRecord(model: String, name: String): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.178.114:8016/xmlrpc/2/object")
                val client = XMLRPCClient(url)
                val db = "odoo"
                val uid = 2 // Admin user ID
                val password = "admin"

                val result = client.call(
                    "execute_kw", db, uid, password, model, "name_search",
                    listOf(name), mapOf("limit" to 1)
                )

                if (result is Array<*>) {
                    val record = result.firstOrNull() as? Array<*>
                    record?.get(0) as? Int
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("ConfirmationActivity", "Error searching $model: $name", e)
                null
            }
        }
    }

    private suspend fun searchProductBarcode(productName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.178.114:8016/xmlrpc/2/object")
                val client = XMLRPCClient(url)
                val db = "odoo"
                val uid = 2 // Admin user ID
                val password = "admin"

                val result = client.call(
                    "execute_kw", db, uid, password, "product.product", "search_read",
                    listOf(
                        listOf(
                            listOf("name", "=", productName)
                        )
                    ),
                    mapOf("fields" to listOf("barcode"), "limit" to 1)
                )

                if (result is Array<*>) {
                    val product = result.firstOrNull() as? Map<*, *>
                    product?.get("barcode") as? String ?: ""
                } else {
                    ""
                }
            } catch (e: Exception) {
                Log.e("ConfirmationActivity", "Error searching product barcode: $productName", e)
                ""
            }
        }
    }

    private suspend fun createOdooRecord(model: String, values: Map<String, Any?>): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.178.114:8016/xmlrpc/2/object")
                val client = XMLRPCClient(url)
                val db = "odoo"
                val uid = 2 // Admin user ID
                val password = "admin"

                val result = client.call(
                    "execute_kw", db, uid, password, model, "create",
                    listOf(values)
                )

                result as? Int
            } catch (e: Exception) {
                Log.e("ConfirmationActivity", "Error creating $model: $values", e)
                null
            }
        }
    }

    private fun sendToOdoo() {
        lifecycleScope.launch {
            try {
                val url = URL("http://192.168.178.114:8016/xmlrpc/2/common")
                val client = XMLRPCClient(url)

                val db = "odoo"
                val username = "mh.rouissi@gmail.com"
                val password = "admin"

                // Authenticate and get uid
                val uid = withContext(Dispatchers.IO) {
                    client.call("authenticate", db, username, password, emptyList<Any>()) as Int
                }

                Log.d("ConfirmationActivity", "Authenticated UID: $uid")

                // Validate and format the date
                val dateString = intent.getStringExtra("date")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = dateString?.let {
                    try {
                        dateFormat.parse(it)
                        it
                    } catch (e: Exception) {
                        Log.e("ConfirmationActivity", "Invalid date format: $it", e)
                        null
                    }
                }

                if (date == null) {
                    throw Exception("Invalid date format. Please use 'yyyy-MM-dd'.")
                }

                // Search for customer and product IDs
                val customerName = intent.getStringExtra("customer") ?: ""
                val productName = intent.getStringExtra("product") ?: ""

                var customerId = searchOdooRecord("res.partner", customerName)
                if (customerId == null) {
                    // Create new customer if not found
                    customerId = createOdooRecord("res.partner", mapOf("name" to customerName))
                    if (customerId == null) {
                        throw Exception("Failed to create customer: $customerName")
                    }
                }

                var productId = searchOdooRecord("product.product", productName)
                if (productId == null) {
                    // Create new product if not found
                    productId = createOdooRecord("product.product", mapOf("name" to productName))
                    if (productId == null) {
                        throw Exception("Failed to create product: $productName")
                    }
                }

                // Log the data to be sent
                Log.d("ConfirmationActivity", "Invoice Data - Customer: $customerId, Product: $productId, Quantity: ${intent.getStringExtra("quantity")}, Price: ${intent.getStringExtra("price")}, Date: $date")

                // Proceed with creating the invoice
                val result = withContext(Dispatchers.IO) {
                    val url = URL("http://192.168.178.114:8016/xmlrpc/2/object")
                    val client = XMLRPCClient(url)

                    val model = "account.move"
                    val method = "create"

                    val invoiceVals = mapOf(
                        "move_type" to "out_invoice",
                        "partner_id" to customerId,
                        "invoice_date" to date,
                        "invoice_line_ids" to listOf(listOf(
                            0, 0, mapOf(
                                "product_id" to productId,
                                "quantity" to intent.getStringExtra("quantity")?.toDoubleOrNull(),
                                "price_unit" to intent.getStringExtra("price")?.toDoubleOrNull()
                            )
                        ))
                    )

                    // Log the data being sent to Odoo
                    Log.d("ConfirmationActivity", "Invoice Vals: $invoiceVals")

                    client.call("execute_kw", db, uid, password, model, method, listOf(invoiceVals))
                }

                Log.d("ConfirmationActivity", "Odoo Response: $result")

                if (result is Int) {
                    val intent = Intent(this@ConfirmationActivity, SuccessActivity::class.java).apply {
                        putExtra("invoiceNumber", result.toString())
                    }
                    startActivity(intent)
                    finishAffinity()
                } else {
                    throw Exception("Invoice creation failed: Unexpected response")
                }
            } catch (e: Exception) {
                Log.e("ConfirmationActivity", "Error creating invoice", e)
                Toast.makeText(this@ConfirmationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
