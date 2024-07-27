package com.minago.odoocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

import com.minago.odoocr.InvoiceProcessor
private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("enterInvoice") {
            EnterInvoiceScreen(navController)
        }
        composable("captureImage") {
            CameraScreen(navController) { image, extractedFields ->
                capturedImage = image
                extractedText = extractedFields
                navController.navigate("result")
            }
        }
        composable("result") {
            ResultScreen(navController, capturedImage, extractedText)
        }
    }
}


@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Please choose one of these options",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate("enterInvoice") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("ENTER INVOICE")
        }

        Button(
            onClick = { /* TODO: Implement later */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SCANNED INVOICES")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "designed by minagoTECH Group",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun EnterInvoiceScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Please choose one of these options",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate("captureImage") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("CAPTURE IMAGE")
        }

        Button(
            onClick = { /* TODO: Implement later */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CHOOSE FROM GALLERY")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "designed by minagoTECH Group",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
@Composable

fun CameraScreen(navController: NavController, onImageCaptured: (Bitmap?, Map<String, Any>) -> Unit) {
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Text("CAMERA PREVIEW", modifier = Modifier.align(Alignment.Center))
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                isProcessing = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val result = InvoiceProcessor.processInvoice(context, "invoice_template.jpg")
                        if (result.isNotEmpty()) {
                            Log.d(TAG, "Extracted fields: $result")
                            onImageCaptured(null, result)
                        } else {
                            errorMessage = "No text was extracted. Please try again."
                            Log.e(TAG, "No text extracted from image.")
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.localizedMessage}"
                        Log.e(TAG, "Error during OCR process", e)
                    } finally {
                        isProcessing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Processing..." else "Capture & Extract")
        }
    }
}

@Composable
fun ResultScreen(navController: NavController, capturedImage: Bitmap?, extractedFields: Map<String, Any>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        Text("Extracted Fields:", style = MaterialTheme.typography.titleLarge)
        Text("Partner: ${extractedFields["partner_id"]}")
        Text("Invoice Date: ${extractedFields["invoice_date"]}")

        Text("Invoice Lines:", style = MaterialTheme.typography.titleMedium)

        (extractedFields["invoice_line_ids"] as? List<Map<String, Any>>)?.forEach { line ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Product: ${line["name"] ?: "N/A"}")
                    Text("Quantity: ${line["product_uom_qty"] ?: "N/A"}")
                    Text("Unit Price: ${line["price_unit"] ?: "N/A"}")
                    Text("Subtotal: ${line["price_subtotal"] ?: "N/A"}")
                }
            }
        }
    }
}